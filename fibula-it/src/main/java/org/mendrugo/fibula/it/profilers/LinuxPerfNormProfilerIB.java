package org.mendrugo.fibula.it.profilers;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class LinuxPerfNormProfilerIB
{
    @Benchmark
    public void work()
    {
        somethingInTheMiddle();
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void somethingInTheMiddle()
    {
        Blackhole.consumeCPU(1);
    }
}
