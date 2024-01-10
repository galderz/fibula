package org.mendrugo.fibula.bootstrap;

import org.mendrugo.fibula.results.JmhOptionals;
import org.mendrugo.fibula.results.NativeBenchmarkParams;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

final class NativeOptions
{
    private static final PackageMode DEFAULT_PACKAGE_MODE = PackageMode.JVM;
    private static final boolean DEFAULT_DECOMPILE = false;

    private final Options jmhOptions;
    private final PackageMode packageMode;
    private final boolean isDecompile;

    NativeOptions(Options jmhOptions)
    {
        this.jmhOptions = jmhOptions;
        this.packageMode = packageModeOrDefault(jmhOptions);
        this.isDecompile = decompileOrDefault(jmhOptions);
    }

    PackageMode getPackageMode()
    {
        return packageMode;
    }

    boolean isDecompile()
    {
        return isDecompile;
    }

    BenchmarkParams getBenchmarkParams(NativeBenchmarkParams nativeParams)
    {
        final IterationParams warmup = new IterationParams(
            IterationType.WARMUP
            , nativeParams.getWarmupIterations(JmhOptionals.fromJmh(jmhOptions.getWarmupIterations()))
            , nativeParams.getWarmupTime(JmhOptionals.fromJmh(jmhOptions.getWarmupTime()))
            , Defaults.WARMUP_BATCHSIZE
        );

        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , nativeParams.getMeasurementIterations(JmhOptionals.fromJmh(jmhOptions.getMeasurementIterations()))
            , nativeParams.getMeasurementTime(JmhOptionals.fromJmh(jmhOptions.getMeasurementTime()))
            , Defaults.MEASUREMENT_BATCHSIZE
        );
        final WorkloadParams params = new WorkloadParams();

        String jdkVersion = System.getProperty("java.version");
        String vmVersion = System.getProperty("java.vm.version");
        String vmName = System.getProperty("java.vm.name");

        return new BenchmarkParams(
            nativeParams.getBenchmark()
            , ""
            , true
            , 1
            , new int[]{1}
            , Collections.emptyList()
            , nativeParams.getMeasurementForks(JmhOptionals.fromJmh(jmhOptions.getForkCount()))
            , 0
            , warmup
            , measurement
            , Mode.Throughput
            , params
            , TimeUnit.SECONDS
            , 1
            , ""
            , new ArrayList<>()
            , jdkVersion
            , vmName
            , vmVersion
            , "0.1"
            , TimeValue.minutes(10)
        );
    }

    private static PackageMode packageModeOrDefault(Options jmhOptions)
    {
        final Optional<Collection<String>> packageMode = jmhOptions.getParameter("fibula.package.mode");
        if (packageMode.hasValue())
        {
            return PackageMode.valueOf(packageMode.get().iterator().next().toUpperCase());
        }

        return DEFAULT_PACKAGE_MODE;
    }

    private boolean decompileOrDefault(Options jmhOptions)
    {
        final Optional<Collection<String>> packageMode = jmhOptions.getParameter("fibula.decompile");
        if (packageMode.hasValue())
        {
            return Boolean.parseBoolean(packageMode.get().iterator().next().toUpperCase());
        }

        return DEFAULT_DECOMPILE;
    }
}
