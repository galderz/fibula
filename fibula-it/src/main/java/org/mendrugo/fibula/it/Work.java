package org.mendrugo.fibula.it;

import java.util.concurrent.TimeUnit;

public final class Work
{
    public static void work()
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private Work()
    {
    }
}
