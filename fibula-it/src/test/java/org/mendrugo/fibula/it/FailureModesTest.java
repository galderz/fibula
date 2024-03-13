package org.mendrugo.fibula.it;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class FailureModesTest
{
    @Inject
    BenchmarkService benchmarkService;

    @Test
    public void shouldNotFailOnErrorByDefault() throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(FailAtBenchmark.class.getCanonicalName())
            .forks(1)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .build();

        final Collection<RunResult> results = benchmarkService.run(opt);
        assertThat(results.isEmpty(), is(true));
    }

    @Test
    public void shouldFailOnErrorAtBenchmark()
    {
        final Options opt = new OptionsBuilder()
            .include(FailAtBenchmark.class.getCanonicalName())
            .forks(1)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .shouldFailOnError(true)
            .build();

        try
        {
            benchmarkService.run(opt);
            throw new AssertionError("Expected exception to be thrown");
        }
        catch (RunnerException e)
        {
            final BenchmarkException cause = (BenchmarkException) e.getCause();
            final Throwable suppressed = cause.getSuppressed()[0];
            assertThat(suppressed, is(instanceOf(IllegalStateException.class)));
            assertThat(suppressed.getMessage(), is("Provoke exception in @Benchmark"));
        }
    }

    // todo add test to fail from @Setup
    // todo add test to fail from @Teardown
}
