package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.ThroughputResult;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class ThroughputFunction implements Function<Infrastructure, ThroughputResult>
{
    @Override
    public ThroughputResult apply(Infrastructure infrastructure)
    {
        long operations = 0;
        long startTime = System.nanoTime();
        do
        {
            doOperation();
            operations++;
        }
        while(!infrastructure.isDone);
        long stopTime = System.nanoTime();

        long duration = stopTime - startTime;
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        double statistic = ((double) operations) * TimeUnit.NANOSECONDS.convert(1, timeUnit) / duration;
        return new ThroughputResult("blah", operations, statistic, timeUnit);
    }

    public abstract void doOperation();
}
