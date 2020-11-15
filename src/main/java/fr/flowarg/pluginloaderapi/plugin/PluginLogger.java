package fr.flowarg.pluginloaderapi.plugin;

import fr.flowarg.flowlogger.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PluginLogger extends Logger
{
    private final String plPrefix;

    public PluginLogger(String plName, String prefix)
    {
        super(prefix, null);
        this.plPrefix = String.format("[%s] ", plName);
    }

    @Override
    public void message(boolean err, String toWrite)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + this.plPrefix + (err ? "[ERROR]: " : "[INFO]: ") + toWrite;
        if (err) System.out.println(EnumLogColor.RED.getColor() + msg + EnumLogColor.RESET.getColor());
        else System.out.println(msg);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void infoColor(EnumLogColor color, String toWrite)
    {
        final String date = String.format("[%s] ",this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + this.plPrefix + "[INFO]: " + toWrite;
        final String colored = color.getColor() + msg + EnumLogColor.RESET.getColor();
        System.out.println(colored);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void warn(String message)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + this.plPrefix + "[WARN]: " + message;
        final String warn = EnumLogColor.YELLOW.getColor() + msg + EnumLogColor.RESET.getColor();
        System.out.println(warn);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void debug(String message)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + this.plPrefix + "[DEBUG]: " + message;
        final String colored = EnumLogColor.CYAN.getColor() + msg + EnumLogColor.RESET.getColor();
        System.out.println(colored);
        this.writeToTheLogFile(msg);
    }

    private String formatDate(Date date)
    {
        return new SimpleDateFormat("hh:mm:ss").format(date);
    }
}
