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
        // Disabling "stop" in console
        PluginLoaderAPI.removeDefaultShutdownTrigger().complete();
        PluginLoaderAPI.addShutdownTrigger(pluginLoaders -> {
            PluginLoaderAPI.getLogger().debug("Custom shutdown trigger: check if a plugin loader is loaded.");
            if(pluginLoaders.get(0).getName().equalsIgnoreCase("Test"))
            {
                PluginLoaderAPI.getLogger().info("Test is loaded, the API can shutting down !");
                return true;
            }
            return false;
        }).complete();
        // Replace "stop" by "exit"
        PluginLoaderAPI.addShutdownTrigger(pluginLoaders -> PluginLoaderAPI.getScanner().nextLine().equalsIgnoreCase("exit")).complete();
        PluginLoaderAPI.ready(Main.class).complete();
    }
}
