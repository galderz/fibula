package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;

public class FibulaSample_01_HelloWorld
{
    @Benchmark
    public void helloWorld()
    {
    }

    public static void main(String[] args)
    {
        System.out.println("First fibula benchmark");
    }
}
