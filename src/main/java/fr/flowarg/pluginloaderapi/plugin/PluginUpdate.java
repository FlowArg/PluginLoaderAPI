package fr.flowarg.pluginloaderapi.plugin;

public class PluginUpdate
{
    private String jarUrl;
    private String crc32Url;
    private boolean ignore;

    public String getJarUrl()
    {
        return this.jarUrl;
    }

    public String getCrc32Url()
    {
        return this.crc32Url;
    }

    public boolean isIgnore()
    {
        return this.ignore;
    }
}
