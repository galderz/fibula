package org.mendrugo.fibula.it;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@QuarkusTest
public class SetupTearDownTest
{
    @Inject
    BenchmarkService benchmarkService;

    @Test
    public void global() throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(SetupTearDownGlobal.class.getCanonicalName())
            .forks(1)
            .measurementIterations(2)
            .measurementTime(TimeValue.milliseconds(200))
            .warmupIterations(0)
            .shouldFailOnError(true)
            .build();

        benchmarkService.run(opt);
    }

    @Test
    public void thread() throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(SetupTearDownThread.class.getCanonicalName())
            .forks(1)
            .measurementIterations(2)
            .measurementTime(TimeValue.milliseconds(200))
            .warmupIterations(0)
            .shouldFailOnError(true)
            .build();

        benchmarkService.run(opt);
    }
}
