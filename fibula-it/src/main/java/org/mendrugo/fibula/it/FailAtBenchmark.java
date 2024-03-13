package org.mendrugo.fibula.it;

import org.openjdk.jmh.annotations.Benchmark;

public class FailAtBenchmark
{
    @Benchmark
    public void failBench()
    {
        throw new IllegalStateException("Provoke exception in @Benchmark");
    }
}
