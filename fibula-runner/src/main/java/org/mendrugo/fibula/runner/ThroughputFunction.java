package org.mendrugo.fibula.runner;

import org.openjdk.jmh.results.RawResults;

import java.util.function.Function;

public abstract class ThroughputFunction implements Function<Infrastructure, RawResults>
{
    @Override
    public RawResults apply(Infrastructure infrastructure)
    {
        final RawResults raw = new RawResults();
        long operations = 0;
        raw.startTime = System.nanoTime();
        do
        {
            doOperation();
            operations++;
        }
        while(!infrastructure.isDone);
        raw.stopTime = System.nanoTime();
        raw.measuredOps = operations;
        return raw;

//        long duration = stopTime - startTime;
//        final TimeUnit timeUnit = TimeUnit.SECONDS;
//        double statistic = ((double) operations) * TimeUnit.NANOSECONDS.convert(1, timeUnit) / duration;
//        return new ThroughputResult("blah", operations, statistic, timeUnit);
    }

    public abstract void doOperation();
}
