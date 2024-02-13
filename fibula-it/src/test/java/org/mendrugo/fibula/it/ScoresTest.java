package org.mendrugo.fibula.it;

import org.junit.jupiter.api.Test;

import java.util.List;

public class ScoresTest
{
    static final String MODE = System.getProperty("fibula.test.mode");

    @Test
    public void jmhSample03_States()
    {
        final List<Result> results = Benchmark.run("JMHSample_03_States", Provider.FIBULA);
        System.out.println(results);
    }
}
