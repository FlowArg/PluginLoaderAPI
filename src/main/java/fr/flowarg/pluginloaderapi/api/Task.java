package fr.flowarg.pluginloaderapi.api;

import java.util.function.Predicate;

public class Task<E>
{
    private final E element;
    private final Predicate<E> taskToQueue;
    private Runnable beforeLoggerAction = () -> {};
    private Runnable afterLoggerAction = () -> {};

    public Task(E element, Predicate<E> taskToQueue)
    {
        this.element = element;
        this.taskToQueue = taskToQueue;
    }

    public Task(E element, Predicate<E> taskToQueue, Runnable loggerAction, LoggerActionType type)
    {
        this.element = element;
        this.taskToQueue = taskToQueue;
        switch (type)
        {
            case AFTER:
                this.afterLoggerAction = loggerAction;
                break;
            case BEFORE:
                this.beforeLoggerAction = loggerAction;
                break;
        }
    }

    public Task(E element, Predicate<E> taskToQueue, Runnable beforeLoggerAction, Runnable afterLoggerAction)
    {
        this.element = element;
        this.taskToQueue = taskToQueue;
        this.beforeLoggerAction = beforeLoggerAction;
        this.afterLoggerAction = afterLoggerAction;
    }

    public void complete()
    {
        this.beforeLoggerAction.run();
        this.taskToQueue.test(this.element);
        this.afterLoggerAction.run();
    }

    public E getElement()
    {
        return this.element;
    }
}
