package org.mendrugo.fibula.results;

public final class Infrastructure
{
    public volatile boolean isDone;
    public boolean lastIteration;

    public void markDone()
    {
        isDone = true;
    }

    public void resetDone()
    {
        isDone = false;
    }

    public void markLastIteration()
    {
        this.lastIteration = true;
    }

    public void resetLastIteration()
    {
        this.lastIteration = false;
    }
}
