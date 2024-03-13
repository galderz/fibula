package org.mendrugo.fibula.it;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class FailureModesTest
{
    @Inject
    BenchmarkService benchmarkService;

    @Test
    public void shouldNotFailByDefault() throws InterruptedException
    {
        final Options opt = new OptionsBuilder()
            .include(FailAtBenchmark.class.getCanonicalName())
            .forks(1)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            // todo why setting warmup iterations to 0 results in no measurement iterations?
            .warmupIterations(1)
            .warmupTime(TimeValue.milliseconds(100))
            .build();

        final Collection<RunResult> results = benchmarkService.run(opt);
        assertThat(results.isEmpty(), is(true));
    }

    // todo add test to fail if instructed via an option
    // todo add test to fail from @Setup
    // todo add test to fail from @Teardown
}
