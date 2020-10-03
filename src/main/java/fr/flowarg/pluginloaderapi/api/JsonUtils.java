package fr.flowarg.pluginloaderapi.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.pluginloaderapi.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class JsonUtils
{
    public static JsonObject toJson(File file)
    {
        final JsonObject result = new JsonObject();
        result.addProperty("name", file.getName());
        try
        {
            result.addProperty("path", file.getCanonicalPath());
        } catch (IOException e)
        {
            result.addProperty("path", file.getAbsolutePath());
        }
        return result;
    }

    public static JsonObject toJson(IAPI api)
    {
        return JsonParser.parseString(api.toJson()).getAsJsonObject();
    }

    public static JsonObject toJson(Plugin plugin)
    {
        return JsonParser.parseString(plugin.toJson()).getAsJsonObject();
    }
}
