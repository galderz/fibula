package org.mendrugo.fibula.it.profilers;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

public class ProfilerIB
{
    @Benchmark
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    public void bench()
    {
        // intentionally left blank
    }
}
