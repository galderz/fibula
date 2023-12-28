package org.mendrugo.fibula.bootstrap;

import org.mendrugo.fibula.results.RunnerArguments;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.Optional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        addIfPresent(RunnerArguments.MEASUREMENT_ITERATIONS, jmhOptions.getMeasurementIterations(), String::valueOf, arguments);
        addIfPresent(RunnerArguments.MEASUREMENT_TIME, jmhOptions.getMeasurementTime(), Object::toString, arguments);
        addIfPresent(RunnerArguments.WARMUP_ITERATIONS, jmhOptions.getWarmupIterations(), String::valueOf, arguments);
        addIfPresent(RunnerArguments.WARMUP_TIME, jmhOptions.getWarmupTime(), Object::toString, arguments);
        // todo pass on logging levels into runner
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

    private <T> void addIfPresent(String paramName, Optional<T> paramValue, Function<T, String> transform, List<String> arguments)
    {
        if (paramValue.hasValue())
        {
            arguments.add("--" + paramName);
            arguments.add(transform.apply(paramValue.get()));
        }
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
