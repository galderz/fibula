package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;

public class FibulaSample_08_NonRegisteredException
{
    @Benchmark
    public void nonRegisteredException()
    {
        throw new IndexOutOfBoundsException("You've gone too far, or came up short");
    }
}
