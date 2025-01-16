package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

final class MultiVmPgoRunner extends Runner
{
    private final NativeImage nativeImage;

    private final Set<BenchmarkParams> instrumented = new HashSet<>();

    public MultiVmPgoRunner(CommandLineOptions options)
    {
        super(options, new MultiVmPgoOutputFormat(options));

        boolean forcePrint = options.verbosity()
            .orElse(Defaults.VERBOSITY)
            .equalsOrHigherThan(VerboseMode.EXTRA);

        this.nativeImage = new NativeImage(Execute.from(forcePrint, this.out));
    }

    @Override
    protected void etaAfterBenchmark(BenchmarkParams benchmarkParams)
    {
        super.etaAfterBenchmark(benchmarkParams);

        if (instrumented.add(benchmarkParams))
        {
            addProfileInformationToBundle();
            rebuildNativeExecutable();
            updateExecutablePath(benchmarkParams);
        }
    }

    private void updateExecutablePath(BenchmarkParams benchmarkParams)
    {
        final BenchmarkParamsReflect reflect = new BenchmarkParamsReflect(benchmarkParams, out);
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

        reflect.amendField("jvm", optimizedBinary.getPath());
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
        out.println("");
        out.println("# PGO: Rebuild bundle with profiling data");
        nativeImage.execute(
            "--bundle-apply=" + Pgo.ENABLED.bundle.getAbsolutePath()
            , "--bundle-create=" + Pgo.ENABLED.bundleOptimized.getAbsolutePath() + ",dry-run"
            , "--pgo"
        );
    }

    private static final class MultiVmPgoOutputFormat extends MultiVmOutputFormat
    {
        private MultiVmPgoOutputFormat(Options options)
        {
            super(options);
        }

        @Override
        public void println(String s)
        {
            if (s.startsWith("# Warmup Fork"))
            {
                out.println("# PGO: Instrumented Warmup Fork");
            }
            out.println(s);
        }
    }
}
