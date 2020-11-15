package fr.flowarg.pluginloaderapi.plugin;

import com.google.gson.JsonObject;
import fr.flowarg.pluginloaderapi.api.IAPI;
import fr.flowarg.pluginloaderapi.api.JsonSerializable;
import fr.flowarg.pluginloaderapi.api.JsonUtils;

import java.io.File;

public abstract class Plugin implements JsonSerializable
{
    private File pluginFile;
    private File dataPluginFolder;
    private PluginLoader pluginLoader;
    private String pluginName;
    private String version;
    private transient PluginLogger logger;
    private IAPI api;

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
    public final PluginLogger getLogger()
    {
        return this.logger;
    }
    public final IAPI getApi()
    {
        return this.api;
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
    final void setLogger(PluginLogger logger)
    {
        this.logger = logger;
    }
    final void setApi(IAPI api)
    {
        this.api = api;
    }

    @Override
    public String toJson()
    {
        final JsonObject result = new JsonObject();
        result.add("pluginFile", JsonUtils.toJson(this.pluginFile));
        result.add("dataPluginFolder", JsonUtils.toJson(this.dataPluginFolder));
        result.addProperty("pluginLoader", this.pluginLoader.getName());
        result.addProperty("pluginName", this.pluginName);
        result.addProperty("version", this.version);
        result.add("api", JsonUtils.toJson(this.api));
        return result.toString();
    }
}
