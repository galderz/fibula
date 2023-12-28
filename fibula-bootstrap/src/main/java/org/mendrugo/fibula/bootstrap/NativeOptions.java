package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.Optional;

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
        return List.of(
            "--iterations"
            , String.valueOf(getMeasurementIterations())
            , "--warmup-iterations"
            , String.valueOf(getWarmupIterations())
        );
    }

    int getMeasurementForks()
    {
        return jmhOptions.getForkCount().orElse(Defaults.MEASUREMENT_FORKS);

//        return jmhOptions.getForkCount().orElse(
//            benchmark.getForks().orElse(
//                Defaults.MEASUREMENT_FORKS));
    }

    int getMeasurementIterations()
    {
        return jmhOptions.getMeasurementIterations().orElse(Defaults.MEASUREMENT_ITERATIONS);

//        jmhOptions.getMeasurementIterations()
//            .orElse(benchmark.getMeasurementIterations()
//            .orElse(benchmark.getMode() == Mode.SingleShotTime ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT : Defaults.MEASUREMENT_ITERATIONS));
    }

    int getWarmupIterations()
    {
        return jmhOptions.getWarmupIterations().orElse(Defaults.WARMUP_ITERATIONS);

//        return jmhOptions.getWarmupIterations().orElse(
//            benchmark.getWarmupIterations().orElse(
//                (benchmark.getMode() == Mode.SingleShotTime) ? Defaults.WARMUP_ITERATIONS_SINGLESHOT : Defaults.WARMUP_ITERATIONS
//            ))
    }

    PackageMode getPackageMode()
    {
        return packageMode;
    }

    boolean isDecompile()
    {
        return isDecompile;
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
