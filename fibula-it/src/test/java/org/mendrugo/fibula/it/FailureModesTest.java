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
    public void shouldFailOnSingleExceptionAtBenchmark()
    {
        // todo make sure only this method is executed
        final Options opt = new OptionsBuilder()
            .include(FailAtBenchmark.class.getCanonicalName() + ".singleException")
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

    @Test
    public void shouldFailOnChainedExceptionAtBenchmark()
    {
        // todo make sure only this method is executed
        final Options opt = new OptionsBuilder()
            .include(FailAtBenchmark.class.getCanonicalName() + ".chainedException")
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
            final Throwable suppressed = e.getCause().getSuppressed()[0];
            assertThat(suppressed, is(instanceOf(RuntimeException.class)));
            assertThat(suppressed.getMessage(), is("Provoke a runtime exception with cause"));
            final Throwable cause = suppressed.getCause();
            assertThat(cause, is(instanceOf(NullPointerException.class)));
            assertThat(cause.getMessage(), is("Provoke null pointer exception"));
        }
    }

    // todo add test to fail from @Setup
    // todo add test to fail from @Teardown
}
