package org.mendrugo.fibula.it.profilers;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;

import java.util.Collection;
import java.util.Collections;

public class ItInternalProfiler implements InternalProfiler
{
    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams)
    {
        // intentionally blank
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result)
    {
        return Collections.emptyList();
    }

    @Override
    public String getDescription()
    {
        return "Integration Test Internal Profiler";
    }
}
