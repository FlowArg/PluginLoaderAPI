package fr.flowarg.pluginloaderapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.pluginloaderapi.api.LoggerActionType;
import fr.flowarg.pluginloaderapi.api.Task;
import fr.flowarg.pluginloaderapi.plugin.PluginLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class PluginLoaderAPI
{
    private PluginLoaderAPI() { throw new UnsupportedOperationException(); }

    private static final List<PluginLoader> PLUGIN_LOADERS = new ArrayList<>();
    private static final List<Class<?>> READY_CLASSES = new ArrayList<>();
    private static final List<Class<?>> AWAIT_READY = new ArrayList<>();
    private static final ILogger LOGGER = new Logger("[PluginLoaderAPI]", null);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Predicate<List<PluginLoader>> DEFAULT_SHUTDOWN_TRIGGER = pluginLoaders -> SCANNER.nextLine().equalsIgnoreCase("stop");
    private static final List<Predicate<List<PluginLoader>>> SHUTDOWN_TRIGGERS = new ArrayList<>();

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(PluginLoaderAPI::shutdown));
        SHUTDOWN_TRIGGERS.add(0, DEFAULT_SHUTDOWN_TRIGGER);
    }

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
        }, () -> LOGGER.debug(String.format("Registering %s plugin loader.", pluginLoader.getName())), LoggerActionType.BEFORE);
    }

    private static Task<PluginLoader> unregisterPluginLoader(PluginLoader pluginLoader)
    {
        return new Task<>(pluginLoader, plLoader -> {
            PLUGIN_LOADERS.remove(plLoader);
            return true;
        }, () -> LOGGER.debug(String.format("Unregistering %s plugin loader.", pluginLoader.getName())), LoggerActionType.BEFORE);
    }

    private static void shutdown()
    {
        final AtomicBoolean flag = new AtomicBoolean(true);

        do {
            flag.set(true);
            SHUTDOWN_TRIGGERS.forEach(listPredicate -> {
                if(!flag.get()) return;
                final boolean temp = listPredicate.test(PLUGIN_LOADERS);
                flag.set(temp);
            });
        }
        while (!flag.get()) ;

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

    public static Scanner getScanner()
    {
        return SCANNER;
    }

    public static Task<Predicate<List<PluginLoader>>> addShutdownTrigger(Predicate<List<PluginLoader>> shutdownTrigger)
    {
        return new Task<>(shutdownTrigger, SHUTDOWN_TRIGGERS::add);
    }

    public static Task<Void> removeDefaultShutdownTrigger()
    {
        return new Task<>(null, unused -> {
            for (int i = 0; i < SHUTDOWN_TRIGGERS.size(); i++)
            {
                if(SHUTDOWN_TRIGGERS.get(i).equals(DEFAULT_SHUTDOWN_TRIGGER))
                    SHUTDOWN_TRIGGERS.remove(i);
            }
            return true;
        });
    }
}
