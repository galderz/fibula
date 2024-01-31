package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class FibulaSample_03_TopLevelState
{
    double x = Math.PI;

    @Benchmark
    public void measure()
    {
        x++;
    }
}
