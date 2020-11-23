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
        this.logger.warn(elements[2].getClassName());
        this.logger.warn(elements[3].getClassName());
        if (!elements[3].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.PluginLoaderAPI") && !elements[3].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.plugin.PluginLoader"))
        {
            this.logger.err(String.format("'Loading plugins' is unavailable from your class (%s). Aborting request...", elements[2].getClassName()));
            return;
        }

        if (!this.loaded)
        {
            try
            {
                this.logger.info("Searching for plugins in : " + this.pluginsDir.getCanonicalPath() + ".");
            } catch (IOException e)
            {
                this.logger.printStackTrace(e);
            }
            new Thread(() -> {
                if (this.pluginsDir.listFiles() != null && this.pluginsDir.listFiles().length > 0)
                {
                    final List<Runnable> launcher = new ArrayList<>();
                    for (File plugin : this.pluginsDir.listFiles())
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
                }
                this.loaded = true;
            }, this.name + " Plugin Loader Thread").start();
        }
    }

    /**
     * Check if a plugin can be update.
     * @param plugin file of plugin
     * @throws IOException if an error occurred
     */
    private void checkForUpdates(File plugin) throws IOException
    {
        final File dir = new File(plugin.getCanonicalPath().replace(".jar", ""));
        boolean flag = true;
        if (dir.listFiles() != null && dir.listFiles().length > 0)
        {
            for (File file : dir.listFiles())
            {
                if (file.getName().equals("update.json"))
                {
                    final String jsonUpdate = FileUtils.loadFile(file);
                    this.downloadUpdate(jsonUpdate, plugin);
                    flag = false;
                }
            }
        }
        else
        {
            final JarFile jarFile = new JarFile(plugin, false, ZipFile.OPEN_READ);
            final ZipEntry entryUpdate = jarFile.getEntry("update.json");
            if(entryUpdate != null)
            {
                final String jsonUpdate = this.getContentFromIS(jarFile.getInputStream(entryUpdate));
                this.downloadUpdate(jsonUpdate, plugin);
                flag = false;
            }
        }
        if (flag) this.logger.warn("No update.json found for: " + plugin.getName());
    }

    private void downloadUpdate(String jsonUpdate, final File plugin) throws IOException
    {
        final PluginUpdate update = PluginLoaderAPI.GSON.fromJson(jsonUpdate, PluginUpdate.class);
        if (!update.isIgnore())
        {
            final String crc32Url = update.getCrc32Url();
            if (crc32Url != null && !crc32Url.trim().equals(""))
            {
                if (this.getContentFromIS(new URL(update.getCrc32Url()).openStream()).equalsIgnoreCase(Long.toString(FileUtils.getCRC32(plugin))))
                    this.logger.info("No update found for: " + plugin.getName());
                else
                {
                    this.logger.info("Update found for: " + plugin.getName() + ", downloading it...");
                    Files.copy(new URL(update.getJarUrl()).openStream(), plugin.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            else this.logger.err("Invalid update.json !! Skipping it...");
        }
        else this.logger.debug("Ignoring update checker for " + plugin.getName());
    }

    private void launchPlugin(PluginManifest manifest, File plugin) throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        final Class<?> pluginClass = Class.forName(manifest.getPluginClass());
        final Plugin chargingPlugin = (Plugin)pluginClass.newInstance();
        chargingPlugin.setPluginFile(plugin);
        chargingPlugin.setPluginLoader(this);
        chargingPlugin.setPluginName(manifest.getName());
        File dataFolder;
        try
        {
            dataFolder = new File(plugin.getCanonicalPath().replace(".jar", ""));
        } catch (IOException e)
        {
            this.logger.err("Cannot use canonical path ! Using absolute path... " + plugin.getAbsolutePath());
            dataFolder = new File(plugin.getAbsolutePath().replace(".jar", ""));
        }
        chargingPlugin.setDataPluginFolder(dataFolder);
        chargingPlugin.setVersion(manifest.getVersion());
        chargingPlugin.setApi(this.api);
        chargingPlugin.setLogger(new PluginLogger(chargingPlugin.getPluginName(), this.logger.getPrefix()));
        chargingPlugin.onStart();
        this.loadedPlugins.add(chargingPlugin);
    }

    private PluginManifest addPluginToClassLoader(JarFile jarFile, ZipEntry entryManifest, File plugin) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final PluginManifest manifest = this.getPluginManifest(jarFile, entryManifest);

        this.logger.info(String.format("Charging plugin '%s' (%s) version %s in '%s' plugin loader on %s (%s).", manifest.getName(), manifest.getPluginClass(), manifest.getVersion(), this.name, Thread.currentThread().getName(), plugin.getName()));
        final Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
        final URLClassLoader urlClassLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        final Method addURL = classLoaderClass.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(urlClassLoader, plugin.toURI().toURL());
        return manifest;
    }

    private PluginManifest getPluginManifest(final JarFile jarFile, final ZipEntry entryManifest) throws IOException
    {
        return PluginLoaderAPI.GSON.fromJson(this.getContentFromIS(jarFile.getInputStream(entryManifest)), PluginManifest.class);
    }

    private String getContentFromIS(InputStream inputStream) throws IOException
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
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
        this.logger.warn("Nothing to reload.");
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
