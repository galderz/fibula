package org.mendrugo.fibula.runner;

import org.openjdk.jmh.results.RawResults;

public final class BenchmarkLifecycle
{
    public static RawResults before()
    {
        final RawResults raw = new RawResults();
        raw.startTime = System.nanoTime();
        return raw;
    }

    public static void after(long operations, RawResults raw)
    {
        raw.stopTime = System.nanoTime();
        raw.measuredOps = operations;
    }
}
