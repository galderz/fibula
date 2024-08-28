package org.mendrugo.fibula.it;

import org.junit.Assert;
import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;

public class BenchmarkModesTest
{
    @Test
    public void benchmarkModes() throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(BenchmarkModes.class.getCanonicalName())
            .shouldFailOnError(true)
            .forks(1)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .build();

        final Collection<RunResult> results = new BenchmarkService().run(opt);
        Assert.assertEquals(3,(results.size()));
        for (RunResult result : results)
        {
            final BenchmarkParams params = result.getParams();
            final String[] benchmarkElements = params.getBenchmark().split("\\.");
            switch (benchmarkElements[benchmarkElements.length - 1])
            {
                case "defaultMode":
                case "throughputMode":
                    Assert.assertEquals(Mode.Throughput, params.getMode());
                    Assert.assertEquals("ops/s", result.getPrimaryResult().getScoreUnit());
                    break;
                case "averageTimeMode":
                    Assert.assertEquals(Mode.AverageTime, params.getMode());
                    Assert.assertEquals("s/op", result.getPrimaryResult().getScoreUnit());
                    break;
                default:
                    throw new AssertionError("Unknonw benchmark: " + params.getBenchmark());
            }
        }

    }
}
