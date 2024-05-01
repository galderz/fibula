package org.mendrugo.fibula.it.profilers;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.it.profilers.ProfilerTestUtils;
import org.openjdk.jmh.profile.LinuxPerfProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Map;

@QuarkusTest
public class LinuxPerfProfilerTest extends org.openjdk.jmh.it.profilers.LinuxPerfProfilerTest
{
    @Inject
    BenchmarkService benchmarkService;

    @Override
    @Test
    public void test() throws RunnerException
    {
        try
        {
            new LinuxPerfProfiler("");
        } catch (ProfilerException e) {
            System.out.println("Profiler is not supported or cannot be enabled, skipping test");
            return;
        }

        Options opts = new OptionsBuilder()
            .include(Fixtures.getTestMask(this.getClass().getSuperclass()))
            .addProfiler(LinuxPerfProfiler.class)
            .build();

        final RunResult rr = benchmarkService.run(opts).iterator().next();

        Map<String, Result> sr = rr.getSecondaryResults();
        String msg = ProfilerTestUtils.checkedGet(sr, "perf").extendedInfo();

        if (sr.containsKey("ipc"))
        {
            double ipc = ProfilerTestUtils.checkedGet(sr, "ipc").getScore();
            double cpi = ProfilerTestUtils.checkedGet(sr, "cpi").getScore();
            Assert.assertNotEquals(0D, ipc, 0D);
            Assert.assertNotEquals(0D, cpi, 0D);
        }

        Assert.assertTrue(msg.contains("cycles"));
        Assert.assertTrue(msg.contains("instructions"));
        Assert.assertTrue(msg.contains("branches"));
    }
}
