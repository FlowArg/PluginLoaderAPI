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

/**
 * PluginLoaderAPI (PLA)
 *
 * @author Flow Arg (FlowArg)
 * @version release-1.0.0
 *
 * Main class of the API, check
 * {@link PluginLoaderAPI#registerPluginLoader(PluginLoader)},
 * {@link PluginLoaderAPI#ready(Class)},
 * {@link PluginLoaderAPI#unregisterPluginLoader(PluginLoader)}
 */
public class PluginLoaderAPI
{
    /**
     * Don't try to instantiate this class, today it's a class only with static methods.
     */
    private PluginLoaderAPI() { throw new UnsupportedOperationException(); }

    private static final List<PluginLoader> PLUGIN_LOADERS = new ArrayList<>();
    private static final List<Class<?>> READY_CLASSES = new ArrayList<>();
    private static final List<Class<?>> AWAIT_READY = new ArrayList<>();
    private static ILogger logger = new Logger("[PluginLoaderAPI]", null);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
    public static final Scanner SCANNER = new Scanner(System.in);
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
            logger.err("PluginLoader : '" + pluginLoader.getName() + "' is already registered !");
            return new Task<>(pluginLoader, () -> plLoader -> false);
        }
        else
        {
            for (PluginLoader x : PLUGIN_LOADERS)
            {
                if(x.getPluginsDir().getAbsolutePath().equals(pluginLoader.getPluginsDir().getAbsolutePath()))
                {
                    logger.err("The plugins directory of '" + pluginLoader.getName() + "' plugin loader is already selected.");
                    return new Task<>(pluginLoader, () -> plLoader -> false);
                }
                else if (x.getName().equals(pluginLoader.getName()))
                {
                    logger.err("PluginLoader : '" + pluginLoader.getName() + "' is already registered !");
                    return new Task<>(pluginLoader, () -> plLoader -> false);
                }
            }
        }
        return new Task<>(pluginLoader, () -> plLoader -> {
            PLUGIN_LOADERS.add(plLoader);
            if(!AWAIT_READY.contains(plLoader.getRegisteredClass()))
                AWAIT_READY.add(plLoader.getRegisteredClass());
            return true;
        }, () -> logger.debug(String.format("Registering %s plugin loader.", pluginLoader.getName())), LoggerActionType.BEFORE);
    }

    private static Task<PluginLoader> unregisterPluginLoader(PluginLoader pluginLoader)
    {
        return new Task<>(pluginLoader, () -> plLoader -> {
            PLUGIN_LOADERS.remove(plLoader);
            return true;
        }, () -> logger.debug(String.format("Unregistering %s plugin loader.", pluginLoader.getName())), LoggerActionType.BEFORE);
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

        logger.info("Shutting down PluginLoaderAPI.");
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
        return new Task<>(null, () -> unused -> {
            for (PluginLoader pluginLoader : PLUGIN_LOADERS)
            {
                if (pluginLoader.isLoaded())
                {
                    logger.err("Could not ready " + clazz.getName() + " class, PluginLoaders are already loaded !");
                    return false;
                }
                else if (pluginLoader.getRegisteredClass().getName().equals(clazz.getName()) && !READY_CLASSES.contains(clazz))
                {
                    READY_CLASSES.add(clazz);
                    logger.debug("Successfully make ready the class: '" + clazz.getName() + "'.");
                }
            }
            if(READY_CLASSES.size() == AWAIT_READY.size())
                PLUGIN_LOADERS.forEach(PluginLoader::loadPlugins);
            return true;
        });
    }

    public static Task<Predicate<List<PluginLoader>>> addShutdownTrigger(Predicate<List<PluginLoader>> shutdownTrigger)
    {
        return new Task<>(shutdownTrigger, () -> SHUTDOWN_TRIGGERS::add);
    }

    public static Task<Void> removeDefaultShutdownTrigger()
    {
        return new Task<>(null, () -> unused -> {
            for (int i = 0; i < SHUTDOWN_TRIGGERS.size(); i++)
            {
                if(SHUTDOWN_TRIGGERS.get(i).equals(DEFAULT_SHUTDOWN_TRIGGER))
                    SHUTDOWN_TRIGGERS.remove(i);
            }
            return true;
        });
    }

    public static boolean finishedLoading()
    {
        final AtomicBoolean result = new AtomicBoolean(true);
        PLUGIN_LOADERS.forEach(pluginLoader -> {
            if(result.get())
            {
                if(pluginLoader.isLoaded())
                {
                    if(pluginLoader.getToLoad() != pluginLoader.getLoadedPlugins().size())
                        result.set(false);
                }
                else result.set(false);
            }
        });
        return result.get();
    }

    public static Task<PluginLoader> reloadPluginLoader(PluginLoader pluginLoader)
    {
        return new Task<>(pluginLoader, () -> active -> {
            active.reload();
            return true;
        });
    }

    public static ILogger getLogger()
    {
        return logger;
    }

    public static void setLogger(ILogger logger)
    {
        PluginLoaderAPI.logger = logger;
    }
}
