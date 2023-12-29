package org.mendrugo.fibula.bootstrap;

import org.mendrugo.fibula.results.JmhOptionals;
import org.mendrugo.fibula.results.NativeBenchmarkParams;
import org.mendrugo.fibula.results.RunnerArguments;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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

    List<String> getRunnerArguments()
    {
        final List<String> arguments = new ArrayList<>();
        addArgumentIfPresent(RunnerArguments.MEASUREMENT_ITERATIONS, jmhOptions.getMeasurementIterations(), String::valueOf, arguments);
        addArgumentIfPresent(RunnerArguments.MEASUREMENT_TIME, jmhOptions.getMeasurementTime(), Object::toString, arguments);
        addArgumentIfPresent(RunnerArguments.WARMUP_ITERATIONS, jmhOptions.getWarmupIterations(), String::valueOf, arguments);
        addArgumentIfPresent(RunnerArguments.WARMUP_TIME, jmhOptions.getWarmupTime(), Object::toString, arguments);
        return arguments;
    }

    PackageMode getPackageMode()
    {
        return packageMode;
    }

    boolean isDecompile()
    {
        return isDecompile;
    }

    Optional<Integer> getMeasurementForks()
    {
        return jmhOptions.getForkCount();
    }

    Optional<Integer> getMeasurementIterations()
    {
        return jmhOptions.getMeasurementIterations();
    }

    BenchmarkParams getBenchmarkParams(NativeBenchmarkParams nativeParams)
    {
        final int measurementForks = nativeParams.getMeasurementForks(JmhOptionals.fromJmh(getMeasurementForks()));
        final int measurementIterations = nativeParams.getMeasurementIterations(JmhOptionals.fromJmh(getMeasurementIterations()));

        final IterationParams warmup = new IterationParams(
            IterationType.WARMUP
            , 0 // Defaults.WARMUP_ITERATIONS
            , Defaults.WARMUP_TIME
            , Defaults.WARMUP_BATCHSIZE
        );
        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , measurementIterations
            , Defaults.MEASUREMENT_TIME
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
            , measurementForks
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

    private <T> void addArgumentIfPresent(String paramName, Optional<T> paramValue, Function<T, String> transform, List<String> arguments)
    {
        if (paramValue.hasValue())
        {
            addArgument(paramName, paramValue.get(), transform, arguments);
        }
    }

    private static <T> void addArgument(String paramName, T value, Function<T, String> transform, List<String> arguments)
    {
        arguments.add("--" + paramName);
        arguments.add(transform.apply(value));
    }
}
