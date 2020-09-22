package fr.flowarg.pluginloaderapi;

import java.util.function.Predicate;

public class Task<E>
{
    private final E element;
    private final Predicate<E> taskToQueue;
    private final Runnable loggerAction;

    public Task(E element, Predicate<E> taskToQueue)
    {
        this.element = element;
        this.taskToQueue = taskToQueue;
        this.loggerAction = () -> {};
    }

    public Task(E element, Predicate<E> taskToQueue, Runnable loggerAction)
    {
        this.element = element;
        this.taskToQueue = taskToQueue;
        this.loggerAction = loggerAction;
    }

    public void complete()
    {
        this.loggerAction.run();
        this.taskToQueue.test(this.element);
    }

    public E getElement()
    {
        return this.element;
    }
}
