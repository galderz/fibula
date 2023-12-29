package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.NativeBenchmarkParams;
import org.openjdk.jmh.runner.BenchmarkListEntry;

public final class Infrastructure
{
    private final NativeBenchmarkParams benchmarkParams;
    private final String benchmark;

    volatile boolean isDone;

    Infrastructure(String benchmark)
    {
        this.benchmark = benchmark;
        this.benchmarkParams = new NativeBenchmarkParams(new BenchmarkListEntry(benchmark));
    }

    void markDone()
    {
        isDone = true;
    }

    void resetDone()
    {
        isDone = false;
    }

    NativeBenchmarkParams getBenchmarkParams()
    {
        return benchmarkParams;
    }

    public String getBenchmark()
    {
        return benchmark;
    }
}
