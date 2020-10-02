package fr.flowarg.pluginloaderapi.plugin;

import fr.flowarg.pluginloaderapi.PluginLoaderAPI;
import fr.flowarg.pluginloaderapi.api.JsonSerializable;

public class PluginUpdate implements JsonSerializable
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

    @Override
    public String toJson()
    {
        return PluginLoaderAPI.getGson().toJson(this);
    }
}
