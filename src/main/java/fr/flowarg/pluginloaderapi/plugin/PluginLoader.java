package fr.flowarg.pluginloaderapi.plugin;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.pluginloaderapi.PluginLoaderAPI;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginLoader
{
    private final String name;
    private final File pluginsDir;
    private final Class<?> registeredClass;
    private final List<Plugin> loadedPlugins = new ArrayList<>();
    private final ILogger logger = PluginLoaderAPI.getLogger();
    private boolean loaded;

    public PluginLoader(String name, File pluginsDir, Class<?> registeredClass)
    {
        this.name = name;
        this.pluginsDir = pluginsDir;
        this.registeredClass = registeredClass;
        this.loaded = false;
    }

    public void loadPlugins()
    {
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if(!elements[3].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.PluginLoaderAPI"))
        {
            this.logger.err(String.format("'Loading plugins' is unavailable from your class (%s). Aborting request...", elements[2].getClassName()));
            return;
        }
        if(!this.loaded)
        {
            this.logger.info("Searching for plugins in : " + pluginsDir.getAbsolutePath() + ".");
            if(this.pluginsDir.listFiles() != null && this.pluginsDir.listFiles().length > 0)
            {
                for (File plugin : this.pluginsDir.listFiles())
                {
                    if(!plugin.isDirectory())
                    {
                        try
                        {
                            final JarFile jarFile = new JarFile(plugin, false, ZipFile.OPEN_READ);
                            final ZipEntry entryManifest = jarFile.getEntry("manifest.json");
                            if(entryManifest != null)
                            {
                                new Thread(() -> {
                                    try
                                    {
                                        this.checkForUpdates(plugin);
                                        this.launchPlugin(this.addPluginToClassLoader(jarFile, entryManifest, plugin), plugin, jarFile);
                                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | IOException e)
                                    {
                                        this.logger.printStackTrace(e);
                                    }
                                }, this.name + " Plugin Loader Thread").start();
                            }
                            else this.logger.warn("manifest.json not found in : " + plugin.getName() + '.');
                        } catch (IOException e)
                        {
                            this.logger.printStackTrace(e);
                        }
                    }
                }
            }
            this.loaded = true;
        }
    }

    private void checkForUpdates(File plugin) throws IOException
    {
        final File dir = new File(plugin.getAbsolutePath().replace(".jar", ""));
        boolean flag = false;
        if(dir.listFiles() != null && dir.listFiles().length > 0)
        {
            for (File file : dir.listFiles())
            {
                if(file.getName().equals("update.json"))
                {
                    final String jsonUpdate = FileUtils.loadFile(file);
                    final PluginUpdate update = PluginLoaderAPI.getGson().fromJson(jsonUpdate, PluginUpdate.class);
                    if(!update.isIgnore())
                    {
                        final String crc32Url = update.getCrc32Url();
                        if(crc32Url != null && !crc32Url.trim().equals(""))
                        {
                            if(this.getContentFromIS(new URL(update.getCrc32Url()).openStream()).equalsIgnoreCase(Long.toString(FileUtils.getCRC32(plugin))))
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
                else flag = true;
            }
        }
        else flag = true;
        if (flag)
            this.logger.warn("No update.json found for: " + plugin.getName());
    }
    
    private void launchPlugin(PluginManifest manifest, File plugin, JarFile jarFile) throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        final Class<?> pluginClass = Class.forName(manifest.getPluginClass());
        final Plugin chargingPlugin = (Plugin)pluginClass.newInstance();
        chargingPlugin.setPluginFile(plugin);
        chargingPlugin.setPluginLoader(this);
        chargingPlugin.setPluginName(manifest.getName());
        chargingPlugin.setDataPluginFolder(new File(plugin.getAbsolutePath().replace(".jar", "")));
        chargingPlugin.setVersion(manifest.getVersion());
        chargingPlugin.setJarFile(jarFile);
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
        return PluginLoaderAPI.getGson().fromJson(this.getContentFromIS(jarFile.getInputStream(entryManifest)), PluginManifest.class);
    }

    private String getContentFromIS(InputStream inputStream) throws IOException
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            sb.append(line);
        return sb.toString();
    }

    public void unloadPlugins()
    {
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if(!elements[2].getClassName().equalsIgnoreCase("fr.flowarg.pluginloaderapi.PluginLoaderAPI"))
        {
            this.logger.err(String.format("'Unloading plugins' is unavailable from your class (%s). Aborting request...", elements[2].getClassName()));
            return;
        }
        if(this.loaded)
            this.loadedPlugins.forEach(Plugin::onStop);
        this.loaded = false;
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
}
