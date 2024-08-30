package org.mendrugo.fibula.it;

import org.junit.Assert;
import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;

public class FailureModesTest
{
    @Test
    public void shouldNotFailOnErrorByDefault() throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(FailAtBenchmark.class.getCanonicalName() + ".avoidFailure")
            .forks(1)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .build();

        final Collection<RunResult> results = new BenchmarkService().run(opt);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void shouldFailOnSingleExceptionAtBenchmark()
    {
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
            new BenchmarkService().run(opt);
            throw new AssertionError("Expected exception to be thrown");
        }
        catch (RunnerException e)
        {
            final BenchmarkException cause = (BenchmarkException) e.getCause();
            final Throwable suppressed = cause.getSuppressed()[0];
            Assert.assertTrue(suppressed instanceof IllegalStateException);
            Assert.assertEquals("Provoke exception in @Benchmark", suppressed.getMessage());
        }
    }

    @Test
    public void shouldFailOnChainedExceptionAtBenchmark()
    {
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
            new BenchmarkService().run(opt);
            throw new AssertionError("Expected exception to be thrown");
        }
        catch (RunnerException e)
        {
            final Throwable suppressed = e.getCause().getSuppressed()[0];
            Assert.assertTrue(suppressed instanceof RuntimeException);
            Assert.assertEquals("Provoke a runtime exception with cause", suppressed.getMessage());
            final Throwable cause = suppressed.getCause();
            Assert.assertTrue(cause instanceof NullPointerException);
            Assert.assertEquals("Provoke null pointer exception", cause.getMessage());
        }
    }

    @Test
    public void shouldFailOnCustomExceptionAtBenchmark()
    {
        final Options opt = new OptionsBuilder()
            .include(FailAtBenchmark.class.getCanonicalName() + ".customException")
            .forks(1)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .shouldFailOnError(true)
            .build();

        try
        {
            new BenchmarkService().run(opt);
            throw new AssertionError("Expected exception to be thrown");
        }
        catch (RunnerException e)
        {
            final BenchmarkException cause = (BenchmarkException) e.getCause();
            final Throwable suppressed = cause.getSuppressed()[0];
            Assert.assertTrue(suppressed instanceof CustomException || suppressed instanceof IllegalStateException);
        }
    }

    // todo add test to fail from @Setup
    // todo add test to fail from @Teardown
}
