package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class FibulaSample_05_BlackholeReturn
{
    double x = Math.PI;

    private double compute(double d)
    {
        for (int c = 0; c < 10; c++)
        {
            d = d * d / Math.PI;
        }
        return d;
    }

    @Benchmark
    public double baseline()
    {
        return compute(x);
    }
}
