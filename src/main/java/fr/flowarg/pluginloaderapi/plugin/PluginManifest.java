package fr.flowarg.pluginloaderapi.plugin;

import fr.flowarg.pluginloaderapi.PluginLoaderAPI;
import fr.flowarg.pluginloaderapi.api.JsonSerializable;

public class PluginManifest implements JsonSerializable
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

    @Override
    public String toJson()
    {
        return PluginLoaderAPI.GSON.toJson(this);
    }
}
