package org.mendrugo.fibula.it;

import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class SetupTearDownTest
{
    @Test
    public void global() throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(SetupTearDownGlobal.class.getCanonicalName())
            .shouldFailOnError(true)
            .forks(1)
            .measurementIterations(2)
            .measurementTime(TimeValue.milliseconds(200))
            .warmupIterations(0)
            .build();

        new BenchmarkService().run(opt);
    }

    @Test
    public void thread() throws RunnerException
    {
        final Options opt = new OptionsBuilder()
            .include(SetupTearDownThread.class.getCanonicalName())
            .shouldFailOnError(true)
            .forks(1)
            .measurementIterations(2)
            .measurementTime(TimeValue.milliseconds(200))
            .warmupIterations(0)
            .build();

        new BenchmarkService().run(opt);
    }
}
