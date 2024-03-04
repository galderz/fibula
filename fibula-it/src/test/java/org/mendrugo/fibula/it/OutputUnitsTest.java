package org.mendrugo.fibula.it;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class OutputUnitsTest
{
    @Inject
    BenchmarkService benchmarkService;

    @Test
    public void allModes() throws InterruptedException
    {
        final Options opt = new OptionsBuilder()
            .include(OutputTimeUnits.class.getCanonicalName())
            .shouldFailOnError(true)
            .forks(1)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(1)
            .warmupTime(TimeValue.milliseconds(100))
            .build();

        final Collection<RunResult> results = benchmarkService.run(opt);
        assertThat(results.size(), is(2));
        for (RunResult result : results)
        {
            final BenchmarkParams params = result.getParams();
            final String[] benchmarkElements = params.getBenchmark().split("\\.");
            switch (benchmarkElements[benchmarkElements.length - 1])
            {
                case "nanoseconds":
                    assertThat(result.getPrimaryResult().getScoreUnit(), is("ops/ns"));
                    break;
                case "milliseconds":
                    assertThat(result.getPrimaryResult().getScoreUnit(), is("ops/ms"));
                    break;
                default:
                    throw new AssertionError("Unknonw benchmark: " + params.getBenchmark());
            }
        }

    }
}
