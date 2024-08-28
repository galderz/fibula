package org.mendrugo.fibula.it.interorder;

import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkStateOrderTest extends org.openjdk.jmh.it.interorder.BenchmarkStateOrderTest
{
    @Override
    @Test
    public void invokeAPI() throws RunnerException
    {
        for (int c = 0; c < Fixtures.repetitionCount(); c++)
        {
            Options opt = new OptionsBuilder()
                .include(Fixtures.getTestMask(this.getClass().getSuperclass()))
                .shouldFailOnError(true)
                .syncIterations(false)
                .build();
            new BenchmarkService().run(opt);
        }
    }
}
