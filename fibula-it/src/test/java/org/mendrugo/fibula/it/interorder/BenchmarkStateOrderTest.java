package org.mendrugo.fibula.it.interorder;

import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.mendrugo.fibula.it.Repetitions;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkStateOrderTest
{
    @Test
    public void invokeAPI() throws RunnerException
    {
        for (int c = 0; c < Repetitions.count(); c++)
        {
            Options opt = new OptionsBuilder()
                .include(BenchmarkStateOrderIB.class.getCanonicalName())
                .shouldFailOnError(true)
                .syncIterations(false)
                .build();
            new BenchmarkService().run(opt);
        }
    }
}
