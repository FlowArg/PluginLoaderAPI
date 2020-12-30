package fr.flowarg.pluginloaderapi.api;

import java.io.Serializable;

/**
 * Extend {@link Serializable} for packet transport.
 */
@FunctionalInterface
public interface JsonSerializable extends Serializable
{
    String toJson();
}
