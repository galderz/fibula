package org.mendrugo.fibula.it.profilers;

import org.junit.Assert;
import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.mendrugo.fibula.it.SecondaryResults;
import org.openjdk.jmh.profile.LinuxPerfProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;

public class LinuxPerfProfilerTest
{
    @Test
    public void test() throws RunnerException
    {
        try
        {
            new LinuxPerfProfiler("");
        }
        catch (ProfilerException e)
        {
            System.out.println("Profiler is not supported or cannot be enabled, skipping test");
            return;
        }

        Options opts = new OptionsBuilder()
            .include(LinuxPerfProfilerIB.class.getCanonicalName())
            .addProfiler(LinuxPerfProfiler.class)
            .shouldFailOnError(true)
            .build();

        final RunResult rr = new BenchmarkService().runSingle(opts);

        Map<String, Result> sr = rr.getSecondaryResults();
        String msg = SecondaryResults.reduce(sr, "perf").extendedInfo();

        if (sr.containsKey("ipc"))
        {
            double ipc = SecondaryResults.reduce(sr, "ipc").getScore();
            double cpi = SecondaryResults.reduce(sr, "cpi").getScore();
            Assert.assertNotEquals(0D, ipc, 0D);
            Assert.assertNotEquals(0D, cpi, 0D);
        }

        Assert.assertTrue(msg.contains("cycles"));
        Assert.assertTrue(msg.contains("instructions"));
        Assert.assertTrue(msg.contains("branches"));
    }
}
