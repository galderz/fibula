package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;

public class FibulaSample_07_FailureCustomException
{
    @Benchmark
    public void sampleCustomException()
    {
        throw new SampleCustomException("Provoke a sample custom exception in @Benchmark");
    }
}
