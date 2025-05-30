package org.mendrugo.fibula;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class BlackholesTest
{
    double x1 = Math.PI;

    @Benchmark
    public double implicitBlackhole()
    {
        return compute(x1);
    }

    @Benchmark
    public void explicitBlackhole(Blackhole bh)
    {
        bh.consume(compute(x1));
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public Object manualBlackholeSubstitutionInvoke() throws Exception
    {
        if (isNativeRun())
        {
            final Class<?> clazz = Class.forName("org.mendrugo.fibula.generated.Target_org_openjdk_jmh_infra_Blackhole");
            final Object blackholeSubstitution = clazz
                .getDeclaredConstructor(String.class)
                .newInstance("Should not be calling this.");
            return clazz.getMethod("consume", double.class).invoke(blackholeSubstitution, x1);
        }
        else
        {
            final String msg = "Benchmark not running in JVM mode";
            System.out.println(msg);
            return msg;
        }
    }

    private static boolean isNativeRun()
    {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    @Test
    public void testImplicitBlackhole() throws RunnerException
    {
        final Consumer<ChainedOptionsBuilder> builder = opts -> opts
            .include(this.getClass().getCanonicalName() + ".implicitBlackhole")
            .shouldFailOnError(true)
            .build();

        CapturingRunner.run(builder);
    }

    @Test
    public void testExplicitBlackhole() throws RunnerException
    {
        final Consumer<ChainedOptionsBuilder> builder = opts -> opts
            .include(this.getClass().getCanonicalName() + ".explicitBlackhole")
            .shouldFailOnError(true)
            .build();

        CapturingRunner.run(builder);
    }

    @Test
    public void testManualBlackholeSubstitutionInvoke() throws RunnerException
    {
        final Consumer<ChainedOptionsBuilder> builder = opts -> opts
            .include(this.getClass().getCanonicalName() + ".manualBlackholeSubstitutionInvoke")
            .shouldFailOnError(true)
            .build();

        CapturingRunner.run(builder);
    }

    private double compute(double d)
    {
        for (int c = 0; c < 10; c++)
        {
            d = d * d / Math.PI;
        }
        return d;
    }
}
