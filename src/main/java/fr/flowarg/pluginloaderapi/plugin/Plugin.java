package fr.flowarg.pluginloaderapi.plugin;

import java.io.File;
import java.util.jar.JarFile;

public abstract class Plugin
{
    private File pluginFile;
    private JarFile jarFile;
    private File dataPluginFolder;
    private PluginLoader pluginLoader;
    private String pluginName;
    private String version;
    private PluginLogger logger;

    public abstract void onStart();
    public abstract void onStop();

    public final File getPluginFile()
    {
        return this.pluginFile;
    }

    public final File getDataPluginFolder()
    {
        return this.dataPluginFolder;
    }

    public final PluginLoader getPluginLoader()
    {
        return this.pluginLoader;
    }

    public final String getPluginName()
    {
        return this.pluginName;
    }

    public final String getVersion()
    {
        return this.version;
    }

    public final JarFile getJarFile()
    {
        return this.jarFile;
    }

    public final PluginLogger getLogger()
    {
        return this.logger;
    }

    final void setPluginFile(File pluginFile)
    {
        this.pluginFile = pluginFile;
    }

    final void setDataPluginFolder(File dataPluginFolder)
    {
        this.dataPluginFolder = dataPluginFolder;
        if(!this.dataPluginFolder.exists())
            this.dataPluginFolder.mkdirs();
    }

    final void setPluginLoader(PluginLoader pluginLoader)
    {
        this.pluginLoader = pluginLoader;
    }

    final void setPluginName(String pluginName)
    {
        this.pluginName = pluginName;
    }

    final void setVersion(String version)
    {
        this.version = version;
    }

    final void setJarFile(JarFile jarFile)
    {
        this.jarFile = jarFile;
    }

    void setLogger(PluginLogger logger)
    {
        this.logger = logger;
    }
}
