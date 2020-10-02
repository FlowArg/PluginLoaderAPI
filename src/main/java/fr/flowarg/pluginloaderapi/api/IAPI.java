package fr.flowarg.pluginloaderapi.api;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface IAPI extends JsonSerializable
{
    IAPI DEFAULT = () -> "default";

    String getAPIName();

    default String toJson()
    {
        final JsonObject result = new JsonObject();
        result.addProperty("name", this.getAPIName());
        return result.toString();
    }
}
