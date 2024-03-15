package org.mendrugo.fibula.it;

import org.openjdk.jmh.annotations.Benchmark;

public class FailAtBenchmark
{
    @Benchmark
    public void singleException()
    {
        throw new IllegalStateException("Provoke exception in @Benchmark");
    }

    @Benchmark
    public void chainedException()
    {
        try
        {
            methodThatThrowsException();
        }
        catch (NullPointerException e)
        {
            throw new RuntimeException("Provoke a runtime exception with cause", e);
        }
    }

    private static void methodThatThrowsException()
    {
        throw new NullPointerException("Provoke null pointer exception");
    }
}
