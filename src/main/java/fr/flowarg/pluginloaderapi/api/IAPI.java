package fr.flowarg.pluginloaderapi.api;

import com.google.gson.JsonObject;

import java.util.function.Supplier;

@FunctionalInterface
public interface IAPI extends JsonSerializable
{
    Supplier<IAPI> DEFAULT = () -> () -> "default";

    String getAPIName();

    default String toJson()
    {
        final JsonObject result = new JsonObject();
        result.addProperty("name", this.getAPIName());
        return result.toString();
    }

    default Supplier<Object> subAPI()
    {
        return () -> null;
    }
}
