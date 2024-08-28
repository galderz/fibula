package org.mendrugo.fibula.it.profilers;

import org.junit.Assert;
import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.it.profilers.ProfilerTestUtils;
import org.openjdk.jmh.profile.LinuxPerfNormProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;

public class LinuxPerfNormProfilerTest extends org.openjdk.jmh.it.profilers.LinuxPerfNormProfilerTest
{
    @Override
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
            .include(Fixtures.getTestMask(this.getClass().getSuperclass()))
            .addProfiler(LinuxPerfNormProfiler.class)
            .build();

        RunResult rr = new BenchmarkService().runSingle(opts);

        Map<String, Result> sr = rr.getSecondaryResults();
        double instructions = ProfilerTestUtils.checkedGet(sr, "instructions", "instructions:u").getScore();
        double cycles = ProfilerTestUtils.checkedGet(sr, "cycles", "cycles:u").getScore();
        double branches = ProfilerTestUtils.checkedGet(sr, "branches", "branches:u").getScore();

        Assert.assertNotEquals(0D, instructions, 0D);
        Assert.assertNotEquals(0D, cycles, 0D);
        Assert.assertNotEquals(0D, branches, 0D);

        if (branches > instructions)
        {
            throw new IllegalStateException(String.format("Branches (%.2f) larger than instructions (%.3f)", branches, instructions));
        }

        double ipc = ProfilerTestUtils.checkedGet(sr, "IPC").getScore();
        double cpi = ProfilerTestUtils.checkedGet(sr, "CPI").getScore();

        Assert.assertNotEquals(0D, ipc, 0D);
        Assert.assertNotEquals(0D, cpi, 0D);
    }
}
