# PluginLoaderAPI

### How to se
First, declare all plugin loaders you need:
```java
public class Main
{
    public static void main(String[] args)
    {
        final PluginLoader testPluginLoader = new PluginLoader("Test", new File(".", "plugins"), Main.class);
        final PluginLoader anotherPluginLoader = new PluginLoader("Another", new File(".", "plugins/another"), Main.class);
    }
}
```
First parameter : the name of the plugin loader (ex: Test)r.
Second parameter : the directory of plugins to load (ex: "./plugins").
Third parameter : the declaring class of plugin loader (ex: Main.class).

Next, register all plugins loader :
```java
public class Main
{
    public static void main(String[] args)
    {
        final PluginLoader testPluginLoader = new PluginLoader("Test", new File(".", "plugins"), Main.class);
        final PluginLoader anotherPluginLoader = new PluginLoader("Another", new File(".", "plugins/another"), Main.class);

        PluginLoaderAPI.registerPluginLoader(testPluginLoader).complete();
        PluginLoaderAPI.registerPluginLoader(anotherPluginLoader).complete();
    }
}
```
Calling `complete` method is needed to execute the task.

Then, ready the declaring class:
```java
public class Main
{
    public static void main(String[] args)
    {
        final PluginLoader testPluginLoader = new PluginLoader("Test", new File(".", "plugins"), Main.class);
        final PluginLoader anotherPluginLoader = new PluginLoader("Another", new File(".", "plugins/another"), Main.class);

        PluginLoaderAPI.registerPluginLoader(testPluginLoader).complete();
        PluginLoaderAPI.registerPluginLoader(anotherPluginLoader).complete();
        PluginLoaderAPI.ready(Main.class).complete();
    }
}
```
