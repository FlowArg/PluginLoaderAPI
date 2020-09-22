package fr.flowarg.anotherplugintest;

import fr.flowarg.pluginloaderapi.PluginLoaderAPI;
import fr.flowarg.pluginloaderapi.plugin.Plugin;

public class AnotherPluginTest extends Plugin
{
    @Override
    public void onStart()
    {
        this.getLogger().info("Starting Plugin " + this.getPluginName() + " !");
        // ERRORS SAMPLE
        this.getLogger().debug("Starting sample errors !");
        this.getPluginLoader().unloadPlugins();
        this.getPluginLoader().loadPlugins();
        PluginLoaderAPI.ready(AnotherPluginTest.class).complete();
        PluginLoaderAPI.registerPluginLoader(this.getPluginLoader()).complete();
        this.getLogger().debug("Ending sample errors !");
    }

    @Override
    public void onStop()
    {
        this.getLogger().info("Stopping Plugin " + this.getPluginName() + " !");
    }
}
