package org.mendrugo.fibula.runner;

public class Infrastructure
{
    public volatile boolean isDone;

    public void markDone()
    {
        isDone = true;
    }

    public void resetDone()
    {
        isDone = false;
    }
}
