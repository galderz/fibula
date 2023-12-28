package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.NativeBenchmarkParams;

public final class Infrastructure
{
    private final NativeBenchmarkParams benchmarkParams;

    volatile boolean isDone;

    Infrastructure(String annotationParams)
    {
        this.benchmarkParams = new NativeBenchmarkParams(annotationParams);
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
}
