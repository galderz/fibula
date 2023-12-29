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

    List<String> getRunnerArguments(int forkIndex)
    {
        final List<String> arguments = new ArrayList<>();
        final Function<Optional<Integer>, Integer> get = Optional::get;
        final Function<Integer, String> valueOf = String::valueOf;
        addArgument(RunnerArguments.FORK_COUNT, jmhOptions.getForkCount(), get.andThen(valueOf), arguments);
        addArgument(RunnerArguments.FORK_INDEX, forkIndex, String::valueOf, arguments);
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
