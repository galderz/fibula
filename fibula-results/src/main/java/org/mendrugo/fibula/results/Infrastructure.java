package org.mendrugo.fibula.results;

public final class Infrastructure
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
