package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain implements QuarkusApplication
{
    @Inject
    ResultService resultService;

    @Inject
    VmInfoService vmInfoService;

    @Override
    public int run(String... args) throws Exception
    {
        Log.debug("Running bootstrap");

        // Read command line arguments just like JMH does
        final Options jmhOptions = new CommandLineOptions(args);
        final NativeOptions options = new NativeOptions(jmhOptions);

        final VmInvoker vmInvoker = VmInvoker.get();
        Log.debugf("VM invoker is: %s", vmInvoker);

        final OutputFormat out = JmhFormats.outputFormat();
        final ProcessRunner processRunner = new ProcessRunner(out);

        final Process infoProcess = processRunner.runInfo(vmInvoker);
        final int infoExitCode = infoProcess.waitFor();
        if (infoExitCode != 0)
        {
            throw new RuntimeException(String.format(
                "Error in process to get VM info (exit code %d)"
                , infoExitCode
            ));
        }

        resultService.startRun(options);

        // Read metadata for all benchmarks
        final SortedSet<BenchmarkParams> benchmarks = options
            .findBenchmarkParams(out)
            .stream()
            .map(params -> applyVmInfo(params, vmInvoker))
            .collect(Collectors.toCollection(TreeSet::new));

        for (BenchmarkParams benchmark : benchmarks)
        {
            out.startBenchmark(benchmark);
            out.println("");

            final int forkCount = benchmark.getMeasurement().getCount();
            for (int i = 0; i < forkCount; i++)
            {
                final Process process = processRunner.runFork(i + 1, benchmark, vmInvoker);
                final int exitCode = process.waitFor();
                if (exitCode != 0)
                {
                    throw new RuntimeException(String.format(
                        "Error in forked runner (exit code %d)"
                        , exitCode
                    ));
                }
            }

            resultService.endBenchmark(benchmark, out);
        }

        resultService.endRun();
        return 0;
    }

    private BenchmarkParams applyVmInfo(BenchmarkParams params, VmInvoker vmInvoker)
    {
        return new BenchmarkParams(
            params.getBenchmark()
            , params.generatedBenchmark()
            , params.shouldSynchIterations()
            , params.getThreads()
            , params.getThreadGroups()
            , params.getThreadGroupLabels()
            , params.getForks()
            , params.getWarmupForks()
            , params.getWarmup()
            , params.getMeasurement()
            , params.getMode()
            , new WorkloadParams() // todo need to bring them from base but not exposed? Order?
            , params.getTimeUnit()
            , params.getOpsPerInvocation()
            , vmInvoker.vm(params.getJvm())
            , params.getJvmArgs()
            , vmInfoService.jdkVersion()
            , vmInfoService.vmName()
            , vmInfoService.vmVersion()
            , params.getJmhVersion()
            , params.getTimeout()
        );
    }
}
