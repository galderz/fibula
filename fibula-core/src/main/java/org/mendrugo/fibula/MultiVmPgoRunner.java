package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

final class MultiVmPgoRunner extends MultiVmRunner
{
    private final NativeImage nativeImage;

    private final Set<BenchmarkParams> instrumented = new HashSet<>();

    public MultiVmPgoRunner(CommandLineOptions options)
    {
        super(options);

        boolean verbosePrint = options.verbosity()
            .orElse(Defaults.VERBOSITY)
            .equalsOrHigherThan(VerboseMode.EXTRA);

        this.nativeImage = new NativeImage(verbosePrint, this.out);
    }

    @Override
    protected void etaAfterBenchmark(BenchmarkParams benchmarkParams)
    {
        super.etaAfterBenchmark(benchmarkParams);

        if (instrumented.add(benchmarkParams))
        {
            out.println("");
            out.println("# PGO: Instrumented warmup fork complete");

            addProfileInformationToBundle();
            rebuildNativeExecutable();
            updateExecutablePath(benchmarkParams);
        }
    }

    private void updateExecutablePath(BenchmarkParams benchmarkParams)
    {
        final File optimizedBinary = Path.of("target")
            .resolve("benchmarks-optimized.output")
            .resolve("default")
            .resolve("benchmarks")
            .toFile();

        if (!optimizedBinary.exists())
        {
            throw new BenchmarkException(
                new IOException("Optimized binary missing: " + optimizedBinary.getPath())
            );
        }

        amendJvm(optimizedBinary.getPath(), benchmarkParams);
    }

    void amendJvm(Object newValue, BenchmarkParams benchmarkParams)
    {
        final String fieldName = "jvm";
        try
        {
            final Class<?> clazz = Class.forName("org.openjdk.jmh.infra.BenchmarkParamsL2");
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(benchmarkParams, newValue);
        }
        catch (Exception e)
        {
            out.println(String.format("Unable to amend benchmark params field %s", fieldName));
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            out.verbosePrintln(stringWriter.toString());
        }
    }

    private void rebuildNativeExecutable()
    {
        out.println("# PGO: Rebuild native from bundle");
        nativeImage.execute(
            "--bundle-apply=" + Pgo.ENABLED.bundleOptimized.getAbsolutePath()
        );
    }

    private void addProfileInformationToBundle()
    {
        out.println("# PGO: Rebuild bundle with profiling data");
        nativeImage.execute(
            "--bundle-apply=" + Pgo.ENABLED.bundle.getAbsolutePath()
            , "--bundle-create=" + Pgo.ENABLED.bundleOptimized.getAbsolutePath() + ",dry-run"
            , "--pgo"
        );
    }
}
