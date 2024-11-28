package org.mendrugo.fibula.it;

import org.junit.jupiter.api.Test;
import org.mendrugo.fibula.MultiVmRunner;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class FailureModesTest
{
    @Benchmark
    public void avoidFailure()
    {
        throw new IllegalArgumentException("Provoke exception in @Benchmark but do not cause failure");
    }

    @Benchmark
    public void singleException()
    {
        throw new IllegalStateException("Provoke exception in @Benchmark");
    }

    @Benchmark
    public void chainedException()
    {
        try
        {
            methodThatThrowsException();
        }
        catch (NullPointerException e)
        {
            throw new RuntimeException("Provoke a runtime exception with cause", e);
        }
    }

    private static void methodThatThrowsException()
    {
        throw new NullPointerException("Provoke null pointer exception");
    }

    @Benchmark
    public void customException()
    {
        throw new CustomException("Provoke a custom exception in @Benchmark");
    }

    @Test
    public void shouldNotFailOnErrorByDefault() throws RunnerException
    {
        final Options opts = new OptionsBuilder()
            .include(this.getClass().getCanonicalName() + ".avoidFailure")
            .forks(0)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .build();

        final Collection<RunResult> results = new MultiVmRunner(opts).run();
        assertTrue(results.isEmpty());
    }

    @Test
    public void shouldFailOnSingleExceptionAtBenchmark()
    {
        final Options opts = new OptionsBuilder()
            .include(this.getClass().getCanonicalName() + ".singleException")
            .shouldFailOnError(true)
            .forks(0)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .verbosity(VerboseMode.EXTRA)
            .build();

        try
        {
            new MultiVmRunner(opts).run();
            throw new AssertionError("Expected exception to be thrown");
        }
        catch (RunnerException e)
        {
            final BenchmarkException cause = (BenchmarkException) e.getCause();
            final Throwable suppressed = cause.getSuppressed()[0];
            assertInstanceOf(IllegalStateException.class, suppressed);
            assertEquals("Provoke exception in @Benchmark", suppressed.getMessage());
        }
    }

    @Test
    public void shouldFailOnChainedExceptionAtBenchmark()
    {
        final Options opts = new OptionsBuilder()
            .include(this.getClass().getCanonicalName() + ".chainedException")
            .shouldFailOnError(true)
            .forks(0)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .build();

        try
        {
            new MultiVmRunner(opts).run();
            throw new AssertionError("Expected exception to be thrown");
        }
        catch (RunnerException e)
        {
            final Throwable suppressed = e.getCause().getSuppressed()[0];
            assertInstanceOf(RuntimeException.class, suppressed);
            assertEquals("Provoke a runtime exception with cause", suppressed.getMessage());
            final Throwable cause = suppressed.getCause();
            assertInstanceOf(NullPointerException.class, cause);
            assertEquals("Provoke null pointer exception", cause.getMessage());
        }
    }

    @Test
    public void shouldFailOnCustomExceptionAtBenchmark()
    {
        final Options opts = new OptionsBuilder()
            .include(this.getClass().getCanonicalName() + ".customException")
            .shouldFailOnError(true)
            .forks(0)
            .measurementIterations(1)
            .measurementTime(TimeValue.milliseconds(100))
            .warmupIterations(0)
            .build();

        final MultiVmRunner runner = new MultiVmRunner(opts);
        try
        {
            runner.run();
            throw new AssertionError("Expected exception to be thrown");
        }
        catch (RunnerException e)
        {
            final BenchmarkException cause = (BenchmarkException) e.getCause();
            final Throwable suppressed = cause.getSuppressed()[0];
            if (runner.isNativeVm())
            {
                assertInstanceOf(IllegalStateException.class, suppressed);
            }
            else
            {
                assertInstanceOf(CustomException.class, suppressed);
                assertEquals("Provoke a custom exception in @Benchmark", suppressed.getMessage());
            }
        }
    }

    // todo add test to fail from @Setup
    // todo add test to fail from @Teardown

    private static boolean isNativeRun()
    {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }
}
