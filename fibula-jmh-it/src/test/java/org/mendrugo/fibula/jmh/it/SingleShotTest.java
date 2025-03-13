package org.mendrugo.fibula.jmh.it;

import org.junit.Test;
import org.mendrugo.fibula.MultiVmRunner;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class SingleShotTest extends org.openjdk.jmh.it.SingleShotTest
{
    @Test
    public void invokeAPI() throws RunnerException
    {
        Options opt = new OptionsBuilder()
            .include(Fixtures.getTestMask(this.getClass()))
            .shouldFailOnError(true)
            .forks(1)
            .build();
        new MultiVmRunner(opt).run();
    }

    @Test
    public void invokeAPI_1() throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(Fixtures.getTestMask(this.getClass()))
            .shouldFailOnError(true)
            .forks(2)
            .build();
        new MultiVmRunner(opt).run();
    }
}
