package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.Optional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        // todo refactor runner parameter names to avoid typos
        addIfPresent("--iterations", jmhOptions.getMeasurementIterations(), arguments);
        addIfPresent("--warmup-iterations", jmhOptions.getWarmupIterations(), arguments);
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

    private void addIfPresent(String paramName, Optional<Integer> paramValue, List<String> arguments)
    {
        if (paramValue.hasValue())
        {
            arguments.add(paramName);
            arguments.add(String.valueOf(paramValue.get()));
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
