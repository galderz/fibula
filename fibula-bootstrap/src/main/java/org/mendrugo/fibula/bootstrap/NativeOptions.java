package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.Optional;

import java.util.Collection;
import java.util.List;

final class NativeOptions
{
    private static final PackageMode DEFAULT_PACKAGE_MODE = PackageMode.JVM;
    // private static final PackageMode DEFAULT_PACKAGE_MODE = PackageMode.NATIVE;

    private final Options jmhOptions;
    private final PackageMode packageMode;

    NativeOptions(Options jmhOptions)
    {
        // Read command line arguments just like JMH does
        this.jmhOptions = jmhOptions;
        this.packageMode = packageModeOrDefault(jmhOptions);
    }

    List<String> getRunnerArguments()
    {
        return List.of(
            "--iterations"
            , String.valueOf(getMeasurementIterations())
        );
    }

    int getMeasurementIterations()
    {
        return jmhOptions.getMeasurementIterations().orElse(Defaults.MEASUREMENT_ITERATIONS);

//        jmhOptions.getMeasurementIterations()
//            .orElse(benchmark.getMeasurementIterations()
//            .orElse(benchmark.getMode() == Mode.SingleShotTime ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT : Defaults.MEASUREMENT_ITERATIONS));
    }

    PackageMode getPackageMode()
    {
        return packageMode;
    }

    private static PackageMode packageModeOrDefault(Options options)
    {
        final Optional<Collection<String>> packageMode = options.getParameter("fibular.package.mode");
        if (packageMode.hasValue())
        {
            return PackageMode.valueOf(packageMode.get().iterator().next());
        }

        return DEFAULT_PACKAGE_MODE;
    }
}
