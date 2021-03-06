package fr.flowarg.pluginloaderapi.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.pluginloaderapi.PluginLoaderAPI;
import fr.flowarg.pluginloaderapi.api.IAPI;
import fr.flowarg.pluginloaderapi.api.JsonSerializable;
import fr.flowarg.pluginloaderapi.api.JsonUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginLoader implements JsonSerializable
{
    private final String name;
    private final File pluginsDir;
    private final Class<?> registeredClass;
    private final List<Plugin> loadedPlugins = new ArrayList<>();
    private final List<String> validExtensions = new ArrayList<>();
    /** Non-Serializable. Logger to use in this plugin loader. */
    private final transient ILogger logger = PluginLoaderAPI.getLogger();
    /** API used by plugins in {@link #pluginsDir} */
    private final IAPI api;
    /** Why loading many times a plugin loader ? */
    private boolean loaded;
    /** Number of plugins to load. */
    private int toLoad;
    /** PLA ClassLoader */
    private URLClassLoader classLoader;

    public PluginLoader(String name, File pluginsDir, Class<?> registeredClass)
    {
        this.name = name;
        this.pluginsDir = pluginsDir;
        this.registeredClass = registeredClass;
        this.loaded = false;
        this.toLoad = 0;
        this.validExtensions.add("jar");
        this.api = IAPI.DEFAULT.get();
    }

    public PluginLoader(String name, File pluginsDir, Class<?> registeredClass, Supplier<IAPI> api)
    {
        this.name = name;
        this.pluginsDir = pluginsDir;
        this.registeredClass = registeredClass;
        this.loaded = false;
        this.toLoad = 0;
        this.validExtensions.add("jar");
        this.api = api.get();
    }

    /**
     * Load plugins from {@link #pluginsDir}
     */
    public void loadPlugins()
    {
        // Prevent unsafe operations.
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if(!elements[2].getClassName().contains("java") && !elements[2].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.plugin.PluginLoader"))
        {            
            this.logger.err(String.format("'Loading plugins' is unavailable from your class (%s). Aborting request...", elements[2].getClassName()));
            return;
        }
        
        if(!elements[3].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.PluginLoaderAPI") && ! elements[3].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.plugin.PluginLoader"))
        {
            this.logger.err(String.format("'Loading plugins' is unavailable from your class (%s). Aborting request...", elements[2].getClassName()));
            return;
        }

        for (StackTraceElement elem : elements)
        {
            if(elem.getMethodName().equalsIgnoreCase("launchPlugin"))
            {
                this.logger.err("'Loading plugins' is unavailable from a launched plugin. Aborting request...");
                return;
            }
        }

        if(this.loaded)
        {
            this.logger.err("Already loaded ! Aborting request...");
            return;
        }

        new Thread(() -> {
            this.logger.info("Searching for plugins in : " + this.pluginsDir.getAbsolutePath() + ".");
            final List<Runnable> launcher = new ArrayList<>();
            for (File plugin : FileUtils.list(this.pluginsDir))
            {
                if (!plugin.isDirectory() && this.isValidExtension(plugin))
                {
                    try
                    {
                        final JarFile jarFile = new JarFile(plugin, false, ZipFile.OPEN_READ);
                        final ZipEntry entryManifest = jarFile.getEntry("manifest.json");
                        if (entryManifest != null)
                        {
                            this.toLoad++;
                            launcher.add(() -> {
                                try
                                {
                                    this.checkForUpdates(plugin);
                                    this.launchPlugin(this.addPluginToClassLoader(jarFile, entryManifest, plugin), plugin);
                                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | IOException e)
                                {
                                    this.logger.printStackTrace(e);
                                }
                            });
                        }
                        else this.logger.warn("manifest.json not found in : " + plugin.getName() + '.');
                    } catch (IOException e)
                    {
                        this.logger.printStackTrace(e);
                    }
                }
            }
            launcher.forEach(Runnable::run);
            this.loaded = true;
        }, this.name + " Plugin Loader Thread").start();
    }

    /**
     * Check if a plugin can be update.
     * @param plugin file of plugin
     * @throws IOException if an error occurred
     */
    private void checkForUpdates(File plugin) throws IOException
    {
        final JarFile jarFile = new JarFile(plugin, false, ZipFile.OPEN_READ);
        final ZipEntry entryUpdate = jarFile.getEntry("update.json");
        if(entryUpdate != null)
        {
            final String jsonUpdate = this.getContentFromIS(jarFile.getInputStream(entryUpdate));
            this.downloadUpdate(jsonUpdate, plugin);
        }
        else this.logger.warn("No update.json found for: " + plugin.getName());
    }

    private void downloadUpdate(String jsonUpdate, final File plugin) throws IOException
    {
        final PluginUpdate update = PluginLoaderAPI.GSON.fromJson(jsonUpdate, PluginUpdate.class);
        if (!update.isIgnore())
        {
            final String crc32Url = update.getCrc32Url();
            if (crc32Url != null && !crc32Url.trim().equals(""))
            {
                final InputStream crc32URLStream = new URL(update.getCrc32Url()).openStream();

                if (this.getContentFromIS(crc32URLStream).trim().equalsIgnoreCase(Long.toString(FileUtils.getCRC32(plugin))))
                    this.logger.info("No update found for: " + plugin.getName());
                else
                {
                    this.logger.info("Update found for: " + plugin.getName() + ", downloading it...");
                    final InputStream jarURLStream = new URL(update.getJarUrl()).openStream();
                    Files.copy(jarURLStream, plugin.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    jarURLStream.close();
                }
                crc32URLStream.close();
            }
            else this.logger.err("Invalid update.json !! Skipping it...");
        }
        else this.logger.debug("Ignoring update checker for " + plugin.getName());
    }

    private void launchPlugin(PluginManifest manifest, File plugin) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        final Class<?> pluginClass = Class.forName(manifest.getPluginClass(), true, this.classLoader);
        final Plugin chargingPlugin = (Plugin)pluginClass.getDeclaredConstructor().newInstance();
        chargingPlugin.setPluginFile(plugin);
        chargingPlugin.setPluginLoader(this);
        chargingPlugin.setPluginName(manifest.getName());
        chargingPlugin.setDataPluginFolder(new File(plugin.getAbsolutePath().replace(".jar", "")));
        chargingPlugin.setVersion(manifest.getVersion());
        chargingPlugin.setApi(this.api);
        chargingPlugin.setLogger(new PluginLogger(chargingPlugin.getPluginName()));
        chargingPlugin.onStart();
        this.loadedPlugins.add(chargingPlugin);
    }

    private PluginManifest addPluginToClassLoader(JarFile jarFile, ZipEntry entryManifest, File plugin) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final PluginManifest manifest = this.getPluginManifest(jarFile, entryManifest);

        this.logger.info(String.format("Charging plugin '%s' (%s) version %s in '%s' plugin loader on %s (%s).", manifest.getName(), manifest.getPluginClass(), manifest.getVersion(), this.name, Thread.currentThread().getName(), plugin.getName()));

        if(this.classLoader == null)
            this.classLoader = new URLClassLoader(
                    new URL[]{plugin.toURI().toURL()},
                    ClassLoader.getSystemClassLoader()
            );
        else
        {
            final Method addURL = this.classLoader.getClass().getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(this.classLoader, plugin.toURI().toURL());
        }

        return manifest;
    }

    private PluginManifest getPluginManifest(final JarFile jarFile, final ZipEntry entryManifest) throws IOException
    {
        return PluginLoaderAPI.GSON.fromJson(this.getContentFromIS(jarFile.getInputStream(entryManifest)), PluginManifest.class);
    }

    private String getContentFromIS(InputStream inputStream) throws IOException
    {
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        final BufferedReader reader = new BufferedReader(inputStreamReader);
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);

        inputStreamReader.close();
        reader.close();
        return sb.toString();
    }

    public void unloadPlugins()
    {
        // Prevent unsafe operations.
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (!elements[2].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.PluginLoaderAPI") && !elements[2].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.plugin.PluginLoader"))
        {
            this.logger.err(String.format("'Unloading plugins' is unavailable from your class (%s). Aborting request...", elements[2].getClassName()));
            return;
        }
        if (this.loaded)
        {
            this.loadedPlugins.forEach(Plugin::onStop);
            this.loadedPlugins.clear();
            System.gc();
        }
        this.loaded = false;
    }

    public void reload()
    {
        // Prevent unsafe operations.
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (!elements[2].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.PluginLoaderAPI"))
        {
            this.logger.err(String.format("'Unloading plugins' is unavailable from your class (%s). Aborting request...", elements[2].getClassName()));
            return;
        }
        if(this.loaded)
        {
            this.unloadPlugins();
            this.loadPlugins();
        }
        else this.logger.warn("Nothing to reload.");
    }

    public boolean isLoaded()
    {
        return this.loaded;
    }

    public String getName()
    {
        return this.name;
    }

    public File getPluginsDir()
    {
        return this.pluginsDir;
    }

    public Class<?> getRegisteredClass()
    {
        return this.registeredClass;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    public IAPI getApi()
    {
        return this.api;
    }

    public int getToLoad()
    {
        return this.toLoad;
    }

    public List<Plugin> getLoadedPlugins()
    {
        return this.loadedPlugins;
    }

    public boolean isValidExtension(File file)
    {
        return this.validExtensions.contains(FileUtils.getFileExtension(file));
    }

    public void addValidExtension(String ext)
    {
        this.validExtensions.add(ext);
    }

    @Override
    public String toJson()
    {
        final JsonObject result = new JsonObject();

        result.addProperty("name", this.name);
        result.add("pluginsDir", JsonUtils.toJson(this.pluginsDir));
        result.addProperty("registeredClass", this.registeredClass.getName());
        final JsonArray array = new JsonArray(this.loadedPlugins.size());
        this.loadedPlugins.forEach(plugin -> array.add(JsonUtils.toJson(plugin)));
        result.add("loadedPlugins", array);
        result.add("api", JsonUtils.toJson(this.api));
        result.addProperty("toLoad", this.toLoad);
        result.addProperty("loaded", this.loaded);

        return result.toString();
    }
}
