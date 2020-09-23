package fr.flowarg.pluginloaderapi.api;

public interface IAPI
{
    IAPI DEFAULT = () -> "default";

    String getAPIName();
}
