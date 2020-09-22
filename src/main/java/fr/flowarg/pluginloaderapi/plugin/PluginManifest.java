package fr.flowarg.pluginloaderapi.plugin;

public class PluginManifest
{
    private String name;
    private String pluginClass;
    private String version;

    public String getName()
    {
        return this.name;
    }

    public String getPluginClass()
    {
        return this.pluginClass;
    }

    public String getVersion()
    {
        return this.version;
    }
}
