package fr.flowarg.pluginloaderapi.api;

import com.google.gson.JsonObject;

import java.io.File;

public class JsonUtils
{
    public static JsonObject toJson(File file)
    {
        final JsonObject result = new JsonObject();
        result.addProperty("name", file.getName());
        result.addProperty("path", file.getAbsolutePath());
        return result;
    }
}
