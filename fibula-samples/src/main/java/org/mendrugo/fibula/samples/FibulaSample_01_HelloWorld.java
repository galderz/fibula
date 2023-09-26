package org.mendrugo.fibula.samples;

import org.mendrugo.fibula.annotations.NativeBenchmark;

public class FibulaSample_01_HelloWorld
{
    @NativeBenchmark
    public void helloWorld()
    {
    }

    public static void main(String[] args)
    {
        System.out.println("First fibula benchmark");
    }
}
