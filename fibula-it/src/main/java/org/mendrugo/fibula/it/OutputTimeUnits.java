package org.mendrugo.fibula.it;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

public class OutputTimeUnits
{
    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void milliseconds()
    {
        Work.work();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void nanoseconds()
    {
        Work.work();
    }
}
