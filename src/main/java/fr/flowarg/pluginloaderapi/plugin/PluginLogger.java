package fr.flowarg.pluginloaderapi.plugin;

import fr.flowarg.flowlogger.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PluginLogger extends Logger
{
    public PluginLogger(String plName)
    {
        super('[' + plName + ']', null);
    }

    @Override
    public void message(boolean err, String toWrite)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + (err ? "[ERROR]: " : "[INFO]: ") + toWrite;
        if (err) System.out.println(EnumLogColor.RED + msg + EnumLogColor.RESET);
        else System.out.println(msg);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void infoColor(EnumLogColor color, String toWrite)
    {
        final String date = String.format("[%s] ",this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + "[INFO]: " + toWrite;
        final String colored = color + msg + EnumLogColor.RESET;
        System.out.println(colored);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void warn(String message)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + "[WARN]: " + message;
        final String warn = EnumLogColor.YELLOW + msg + EnumLogColor.RESET;
        System.out.println(warn);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void debug(String message)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + "[DEBUG]: " + message;
        final String colored = EnumLogColor.CYAN + msg + EnumLogColor.RESET;
        System.out.println(colored);
        this.writeToTheLogFile(msg);
    }

    private String formatDate(Date date)
    {
        return new SimpleDateFormat("hh:mm:ss").format(date);
    }
}
