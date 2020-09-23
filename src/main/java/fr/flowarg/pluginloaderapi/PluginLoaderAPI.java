package fr.flowarg.pluginloaderapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.pluginloaderapi.plugin.PluginLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class PluginLoaderAPI
{
    private PluginLoaderAPI() { throw new UnsupportedOperationException(); }

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(PluginLoaderAPI::shutdown));
    }

    private static final List<PluginLoader> PLUGIN_LOADERS = new ArrayList<>();
    private static final List<Class<?>> READY_CLASSES = new ArrayList<>();
    private static final List<Class<?>> AWAIT_READY = new ArrayList<>();
    private static final ILogger LOGGER = new APILogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    public static Task<PluginLoader> registerPluginLoader(PluginLoader pluginLoader)
    {
        if(PLUGIN_LOADERS.contains(pluginLoader))
        {
            LOGGER.err("PluginLoader : '" + pluginLoader.getName() + "' is already registered !");
            return new Task<>(pluginLoader, plLoader -> false);
        }
        else
        {
            for (PluginLoader x : PLUGIN_LOADERS)
            {
                if(x.getPluginsDir().getAbsolutePath().equals(pluginLoader.getPluginsDir().getAbsolutePath()))
                {
                    LOGGER.err("The plugins directory of '" + pluginLoader.getName() + "' plugin loader is already selected.");
                    return new Task<>(pluginLoader, plLoader -> false);
                }
                else if (x.getName().equals(pluginLoader.getName()))
                {
                    LOGGER.err("PluginLoader : '" + pluginLoader.getName() + "' is already registered !");
                    return new Task<>(pluginLoader, plLoader -> false);
                }
            }
        }
        return new Task<>(pluginLoader, plLoader -> {
            PLUGIN_LOADERS.add(plLoader);
            if(!AWAIT_READY.contains(plLoader.getRegisteredClass()))
                AWAIT_READY.add(plLoader.getRegisteredClass());
            return true;
        }, () -> LOGGER.debug(String.format("Registering %s plugin loader.", pluginLoader.getName())));
    }

    private static Task<PluginLoader> unregisterPluginLoader(PluginLoader pluginLoader)
    {
        return new Task<>(pluginLoader, plLoader -> {
            PLUGIN_LOADERS.remove(plLoader);
            return true;
        }, () -> LOGGER.debug(String.format("Unregistering %s plugin loader.", pluginLoader.getName())));
    }

    private static void shutdown()
    {
        final Scanner scanner = new Scanner(System.in);
        String command;
        do
        {
            command = scanner.nextLine();
        }while (!command.equalsIgnoreCase("stop"));
        scanner.close();
        LOGGER.info("Shutting down PluginLoaderAPI.");
        int lgt = PLUGIN_LOADERS.size();
        for (int i = 0; i < lgt; lgt--)
        {
            final PluginLoader loader = PLUGIN_LOADERS.get(i);
            loader.unloadPlugins();
            unregisterPluginLoader(loader).complete();
        }
        READY_CLASSES.clear();
        AWAIT_READY.clear();
    }

    public static Task<Void> ready(Class<?> clazz)
    {
        return new Task<>(null, unused -> {
            for (PluginLoader pluginLoader : PLUGIN_LOADERS)
                if(pluginLoader.isLoaded())
                {
                    LOGGER.err("Could not ready " + clazz.getName() + " class, PluginLoaders are already loaded !");
                    return false;
                }
                else if (pluginLoader.getRegisteredClass().getName().equals(clazz.getName()) && !READY_CLASSES.contains(clazz))
                    READY_CLASSES.add(clazz);
            if(READY_CLASSES.size() == AWAIT_READY.size())
                loadPlugins();
            return true;
        });
    }

    private static void loadPlugins()
    {
        PLUGIN_LOADERS.forEach(PluginLoader::loadPlugins);
    }

    public static ILogger getLogger()
    {
        return LOGGER;
    }

    public static Gson getGson()
    {
        return GSON;
    }

    private static class APILogger extends Logger
    {
        public APILogger()
        {
            super("[PluginLoaderAPI]", null);
        }

        @Override
        public void debug(String message)
        {
            final String date = String.format("[%s] ", new SimpleDateFormat("hh:mm:ss").format(new Date()));
            final String msg = EnumLogColor.CYAN.getColor() + date + this.getPrefix() + "[DEBUG] " + message + EnumLogColor.RESET.getColor();
            System.out.println(msg);
        }
    }
}
