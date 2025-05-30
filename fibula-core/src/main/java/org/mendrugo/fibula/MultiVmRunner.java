package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.ActionMode;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

public class MultiVmRunner extends Runner
{
    private final ForkedVm forkedVm;

    public MultiVmRunner(Options options)
    {
        super(options);
        this.forkedVm = ForkedVm.instance(out);
    }

    @Override
    protected BenchmarkParams newBenchmarkParams(BenchmarkListEntry benchmark, ActionMode mode)
    {
        final BenchmarkParams bp = super.newBenchmarkParams(benchmark, mode);
        return new BenchmarkParams(
            bp.getBenchmark()
            , bp.generatedBenchmark()
            , bp.shouldSynchIterations()
            , bp.getThreads()
            , bp.getThreadGroups()
            , bp.getThreadGroupLabels()
            , bp.getForks()
            , bp.getWarmupForks()
            , bp.getWarmup()
            , bp.getMeasurement()
            , bp.getMode()
            , benchmark.getWorkloadParams()
            , bp.getTimeUnit()
            , bp.getOpsPerInvocation()
            , forkedVm.executablePath(bp.getJvm())
            , forkedVm.jvmArgs(bp)
            , forkedVm.jdkVersion()
            , forkedVm.vmName()
            , forkedVm.vmVersion()
            // todo change to "fibula:" + new Version().getVersion()
            , "fibula:999-SNAPSHOT"
            , bp.getTimeout()
        );
    }

    static boolean isNativeVm()
    {
        return new MultiVmRunner(
            new OptionsBuilder()
                .verbosity(VerboseMode.EXTRA)
                .build()
        ).forkedVm.isNativeVm();
    }
}
