package org.mendrugo.fibula.bootstrap;

import org.mendrugo.fibula.results.JmhOptionals;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

final class NativeOptions
{
    private static final PackageMode DEFAULT_PACKAGE_MODE = PackageMode.NATIVE;

    private final Options jmhOptions;
    private final PackageMode packageMode;

    NativeOptions(Options jmhOptions)
    {
        this.jmhOptions = jmhOptions;
        this.packageMode = packageModeOrDefault(jmhOptions);
    }

    PackageMode getPackageMode()
    {
        return packageMode;
    }

    BenchmarkParams getBenchmarkParams(BenchmarkListEntry benchmark)
    {
        final IterationParams warmup = new IterationParams(
            IterationType.WARMUP
            , getWarmupIterations(benchmark, JmhOptionals.fromJmh(jmhOptions.getWarmupIterations()))
            , getWarmupTime(benchmark, JmhOptionals.fromJmh(jmhOptions.getWarmupTime()))
            , Defaults.WARMUP_BATCHSIZE
        );

        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , getMeasurementIterations(benchmark, JmhOptionals.fromJmh(jmhOptions.getMeasurementIterations()))
            , getMeasurementTime(benchmark, JmhOptionals.fromJmh(jmhOptions.getMeasurementTime()))
            , Defaults.MEASUREMENT_BATCHSIZE
        );
        final WorkloadParams params = new WorkloadParams();

        String jdkVersion = System.getProperty("java.version");
        String vmVersion = System.getProperty("java.vm.version");
        String vmName = System.getProperty("java.vm.name");

        return new BenchmarkParams(
            benchmark.getUsername()
            , ""
            , true
            , 1
            , new int[]{1}
            , Collections.emptyList()
            , getMeasurementForks(benchmark, JmhOptionals.fromJmh(jmhOptions.getForkCount()))
            , 0
            , warmup
            , measurement
            , Mode.Throughput
            , params
            , TimeUnit.SECONDS
            , 1
            , getJvm(benchmark, JmhOptionals.fromJmh(jmhOptions.getJvm()))
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
        if (jmhOptions.getParameter("fibula.package.mode").hasValue())
        {
            return PackageMode.valueOf(jmhOptions.getParameter("fibula.package.mode").get().iterator().next().toUpperCase());
        }

        return DEFAULT_PACKAGE_MODE;
    }

    private static int getMeasurementForks(BenchmarkListEntry benchmark, Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getForks()
            .orElse(Defaults.MEASUREMENT_FORKS));
    }

    private static int getMeasurementIterations(BenchmarkListEntry benchmark, java.util.Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getMeasurementIterations()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT
                : Defaults.MEASUREMENT_ITERATIONS)
            );
    }

    private static TimeValue getMeasurementTime(BenchmarkListEntry benchmark, Optional<TimeValue> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getMeasurementTime()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? TimeValue.NONE
                : Defaults.MEASUREMENT_TIME)
            );
    }

    private static int getWarmupIterations(BenchmarkListEntry benchmark, Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getWarmupIterations()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? Defaults.WARMUP_ITERATIONS_SINGLESHOT
                : Defaults.WARMUP_ITERATIONS)
            );
    }

    private static TimeValue getWarmupTime(BenchmarkListEntry benchmark, Optional<TimeValue> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getWarmupTime()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? TimeValue.NONE
                : Defaults.WARMUP_TIME)
            );
    }

    private static String getJvm(BenchmarkListEntry benchmark, Optional<String> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getJvm()
            .orElse(Utils.getCurrentJvm()));
    }
}
