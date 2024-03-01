package org.mendrugo.fibula.it;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

// @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
// @Warmup(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
// todo add test to verify @Measurement and @Warmup annotation values are respected
public class BenchmarkModes
{
    @Benchmark
    public void defaultMode()
    {
        Work.work();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void throughputMode()
    {
        Work.work();
    }
}
