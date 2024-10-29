package org.mendrugo.fibula.it.profilers;

import org.junit.Assert;
import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.mendrugo.fibula.it.SecondaryResults;
import org.openjdk.jmh.profile.LinuxPerfNormProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;

public class LinuxPerfNormProfilerTest
{
    @Test
    public void test() throws RunnerException
    {
        try
        {
            new LinuxPerfNormProfiler("");
        }
        catch (ProfilerException e)
        {
            System.out.println("Profiler is not supported or cannot be enabled, skipping test");
            return;
        }

        Options opts = new OptionsBuilder()
            .include(LinuxPerfProfilerIB.class.getCanonicalName())
            .shouldFailOnError(true)
            .addProfiler(LinuxPerfNormProfiler.class)
            .build();

        RunResult rr = new BenchmarkService().runSingle(opts);

        Map<String, Result> sr = rr.getSecondaryResults();
        double instructions = SecondaryResults.reduce(sr, "instructions", "instructions:u").getScore();
        double cycles = SecondaryResults.reduce(sr, "cycles", "cycles:u").getScore();
        double branches = SecondaryResults.reduce(sr, "branches", "branches:u").getScore();

        Assert.assertNotEquals(0D, instructions, 0D);
        Assert.assertNotEquals(0D, cycles, 0D);
        Assert.assertNotEquals(0D, branches, 0D);

        if (branches > instructions)
        {
            throw new IllegalStateException(String.format("Branches (%.2f) larger than instructions (%.3f)", branches, instructions));
        }

        double ipc = SecondaryResults.reduce(sr, "IPC").getScore();
        double cpi = SecondaryResults.reduce(sr, "CPI").getScore();

        Assert.assertNotEquals(0D, ipc, 0D);
        Assert.assertNotEquals(0D, cpi, 0D);
    }
}
