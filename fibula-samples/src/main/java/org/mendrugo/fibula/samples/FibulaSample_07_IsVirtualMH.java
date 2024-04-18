package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class FibulaSample_07_IsVirtualMH
{
    private static final MethodHandle VIRTUAL_MH = findVirtualMH();

    @Benchmark
    public boolean directCall()
    {
        return Thread.currentThread().isVirtual();
    }

    @Benchmark
    public boolean methodHandleCall() throws Throwable
    {
        return (boolean) VIRTUAL_MH.invokeExact(Thread.currentThread());
    }

    private static MethodHandle findVirtualMH()
    {
        try
        {
            return MethodHandles.publicLookup().findVirtual(
                Thread.class
                , "isVirtual"
                , MethodType.methodType(boolean.class)
            );
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
