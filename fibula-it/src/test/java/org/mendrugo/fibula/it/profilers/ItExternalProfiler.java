package org.mendrugo.fibula.it.profilers;

import org.junit.Assert;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class ItExternalProfiler implements ExternalProfiler
{
    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params)
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params)
    {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams)
    {
        // intentionally left blank
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr)
    {
        Assert.assertNotEquals("Forked VM PID is not 0", 0, pid);
        return Collections.emptyList();
    }

    @Override
    public boolean allowPrintOut()
    {
        return true;
    }

    @Override
    public boolean allowPrintErr()
    {
        return true;
    }

    @Override
    public String getDescription()
    {
        return "Integration Test External Profiler";
    }
}
