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
        if (err) System.out.printf("%s%s%s\n", EnumLogColor.RED, msg, EnumLogColor.RESET);
        else System.out.printf("%s\n", msg);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void infoColor(EnumLogColor color, String toWrite)
    {
        final String date = String.format("[%s] ",this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + "[INFO]: " + toWrite;
        final String colored = color + msg + EnumLogColor.RESET;
        System.out.printf("%s\n", colored);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void warn(String message)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + "[WARN]: " + message;
        final String warn = EnumLogColor.YELLOW + msg + EnumLogColor.RESET;
        System.out.printf("%s\n", warn);
        this.writeToTheLogFile(msg);
    }

    @Override
    public void debug(String message)
    {
        final String date = String.format("[%s] ", this.formatDate(new Date()));
        final String msg = date + this.getPrefix() + "[DEBUG]: " + message;
        final String colored = EnumLogColor.CYAN + msg + EnumLogColor.RESET;
        System.out.printf("%s\n", colored);
        this.writeToTheLogFile(msg);
    }

    private String formatDate(Date date)
    {
        return new SimpleDateFormat("hh:mm:ss").format(date);
    }
}
