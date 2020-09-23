package fr.flowarg.sampleimplentation;

import fr.flowarg.pluginloaderapi.PluginLoaderAPI;
import fr.flowarg.pluginloaderapi.plugin.PluginLoader;

import java.io.File;

public class Main
{
    public static void main(String[] args)
    {
        final PluginLoader testPluginLoader = new PluginLoader("Test", new File(".", "plugins"), Main.class);
        final PluginLoader anotherPluginLoader = new PluginLoader("Another", new File(".", "plugins/another"), Main.class, new APIImplementation());
        PluginLoaderAPI.registerPluginLoader(testPluginLoader).complete();
        PluginLoaderAPI.registerPluginLoader(anotherPluginLoader).complete();
        PluginLoaderAPI.ready(Main.class).complete();
    }
}
