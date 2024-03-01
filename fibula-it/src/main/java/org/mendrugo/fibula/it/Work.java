package org.mendrugo.fibula.it;

import java.util.concurrent.TimeUnit;

final class Work
{
    static void work()
    {
        // courtesy for parallel-running tests
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
