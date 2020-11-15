package fr.flowarg.pluginloaderapi.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Task<E> implements JsonSerializable
{
    private static final Runnable DEFAULT_LOGGER_ACTION = () -> {};
    private final E element;
    private final transient Predicate<E> taskToQueue;
    private Runnable beforeLoggerAction = DEFAULT_LOGGER_ACTION;
    private Runnable afterLoggerAction = DEFAULT_LOGGER_ACTION;
    private boolean executed;

    public Task(E element, Supplier<Predicate<E>> taskToQueue)
    {
        this.element = element;
        this.taskToQueue = taskToQueue.get();
        this.executed = false;
    }

    public Task(E element, Supplier<Predicate<E>> taskToQueue, Runnable loggerAction, LoggerActionType type)
    {
        this.element = element;
        this.taskToQueue = taskToQueue.get();
        switch (type)
        {
            case AFTER:
                this.afterLoggerAction = loggerAction;
                break;
            case BEFORE:
                this.beforeLoggerAction = loggerAction;
                break;
        }
        this.executed = false;
    }

    public Task(E element, Supplier<Predicate<E>> taskToQueue, Runnable beforeLoggerAction, Runnable afterLoggerAction)
    {
        this.element = element;
        this.taskToQueue = taskToQueue.get();
        this.beforeLoggerAction = beforeLoggerAction;
        this.afterLoggerAction = afterLoggerAction;
        this.executed = false;
    }

    public void complete()
    {
        this.executed = true;
        this.beforeLoggerAction.run();
        if(this.taskToQueue.test(this.element))
            this.afterLoggerAction.run();
    }

    public E getElement()
    {
        return this.element;
    }

    public boolean isExecuted()
    {
        return this.executed;
    }

    @Override
    public String toJson()
    {
        final JsonObject result = new JsonObject();
        if (this.element instanceof JsonSerializable)
            result.add("element", JsonParser.parseString(((JsonSerializable)this.element).toJson()));
        else result.addProperty("element", this.element != null ? this.element.toString() : "null");

        result.addProperty("beforeLoggerAction", !this.beforeLoggerAction.equals(DEFAULT_LOGGER_ACTION));
        result.addProperty("afterLoggerAction", !this.afterLoggerAction.equals(DEFAULT_LOGGER_ACTION));
        result.addProperty("executed", this.executed);
        return result.toString();
    }
}
