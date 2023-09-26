package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.ThroughputResult;

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
        return ThroughputResult.of("blah", operations, stopTime, startTime);
    }

    abstract void doOperation();
}
