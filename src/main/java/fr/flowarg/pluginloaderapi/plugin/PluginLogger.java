package fr.flowarg.pluginloaderapi.plugin;

import fr.flowarg.flowlogger.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PluginLogger extends Logger
{
    private final String plName;
    private final String plPrefix;

    public PluginLogger(String plName, String prefix)
    {
        super(prefix, null);
        this.plName = plName;
        this.plPrefix = String.format("[%s] ", this.plName);
    }

    private void message(boolean err, String toWrite)
    {
        final String date = String.format("[%s] ", new SimpleDateFormat("hh:mm:ss").format(new Date()));
        final String msg = date + this.getPrefix() + this.plPrefix + (err ? "[ERROR] " : "[INFO] ") + toWrite;
        if (err) System.err.println(msg);
        else System.out.println(msg);
    }

    @Override
    public void info(String message)
    {
        this.message(false, message);
    }

    @Override
    public void err(String message)
    {
        this.message(true, message);
    }

    @Override
    public void infoColor(EnumLogColor color, String toWrite)
    {
        final String date = String.format("[%s] ", new SimpleDateFormat("hh:mm:ss").format(new Date()));
        final String msg = color.getColor() + date + this.getPrefix() + this.plPrefix + "[INFO] " + toWrite + EnumLogColor.RESET.getColor();
        System.out.println(msg);
    }

    @Override
    public void warn(String message)
    {
        final String date = String.format("[%s] ", new SimpleDateFormat("hh:mm:ss").format(new Date()));
        final String warn = EnumLogColor.YELLOW.getColor() + date + this.getPrefix() + this.plPrefix + "[WARN] " + message + EnumLogColor.RESET.getColor();
        System.out.println(warn);
    }

    @Override
    public void debug(String message)
    {
        final String date = String.format("[%s] ", new SimpleDateFormat("hh:mm:ss").format(new Date()));
        final String msg = EnumLogColor.CYAN.getColor() + date + this.getPrefix() + this.getPlPrefix() + "[DEBUG] " + message + EnumLogColor.RESET.getColor();
        System.out.println(msg);
    }

    public String getPlName()
    {
        return this.plName;
    }

    public String getPlPrefix()
    {
        return this.plPrefix;
    }
}
