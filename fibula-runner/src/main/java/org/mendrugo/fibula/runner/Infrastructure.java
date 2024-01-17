package org.mendrugo.fibula.runner;

public final class Infrastructure
{
    volatile boolean isDone;

    void markDone()
    {
        isDone = true;
    }

    void resetDone()
    {
        isDone = false;
    }
}
