package org.mendrugo.fibula.samples;

import org.mendrugo.fibula.annotations.FibulaBenchmark;

public class FibulaSample_01_HelloWorld
{
    @FibulaBenchmark
    public void helloWorld()
    {
    }

    public static void main(String[] args)
    {
        System.out.println("First fibula benchmark");
    }
}
