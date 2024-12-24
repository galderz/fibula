package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.VerboseMode;

final class MultiVmPgoRunner extends Runner
{
    private final NativeImage nativeImage;

    boolean instrumentRunCompleted;

    public MultiVmPgoRunner(CommandLineOptions options)
    {
        super(options, new MultiVmOutputFormat(options));

        boolean forcePrint = options.verbosity()
            .orElse(Defaults.VERBOSITY)
            .equalsOrHigherThan(VerboseMode.EXTRA);

        this.nativeImage = new NativeImage(Execute.from(forcePrint, this.out));
    }

    @Override
    protected void etaBeforeBenchmark()
    {
        super.etaBeforeBenchmark();

        if (!instrumentRunCompleted)
        {
            out.println("# PGO: Instrumented Warmup Fork");
        }
    }

    @Override
    protected void etaAfterBenchmark(BenchmarkParams benchmarkParams)
    {
        super.etaAfterBenchmark(benchmarkParams);

        // The end of the very first run is the end instrumentation run.
        // Rebuild the native binary before executing next fork.
        if (!instrumentRunCompleted)
        {
            instrumentRunCompleted = true;
            addProfileInformationToBundle();
            // todo add any debugging/profiling parameters here?
            rebuildNativeExecutable();
            updateExecutablePath(benchmarkParams);
        }
    }

    private void updateExecutablePath(BenchmarkParams benchmarkParams)
    {
        final ForkedVm forkedVm = ForkedVm.instance();
        final BenchmarkParamsReflect reflect = new BenchmarkParamsReflect(benchmarkParams, out);
        reflect.amendField("jvm", forkedVm.executablePath(benchmarkParams.getJvm()));
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
}
