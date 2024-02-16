package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.JmhFormats;
import org.mendrugo.fibula.results.JmhOptionals;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.Collections;
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

        final OutputFormat out = JmhFormats.outputFormat();
        final ProcessRunner processRunner = new ProcessRunner(out);

        final Process infoProcess = processRunner.runInfo();
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
            .map(this::applyVmInfo)
            .collect(Collectors.toCollection(TreeSet::new));

        for (BenchmarkParams benchmark : benchmarks)
        {
            out.startBenchmark(benchmark);
            out.println("");

            final int forkCount = benchmark.getMeasurement().getCount();
            for (int i = 0; i < forkCount; i++)
            {
                final Process process = processRunner.runFork(i + 1, benchmark);
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

    private BenchmarkParams applyVmInfo(BenchmarkParams base)
    {
        return new BenchmarkParams(
            base.getBenchmark()
            , base.generatedBenchmark()
            , base.shouldSynchIterations()
            , base.getThreads()
            , base.getThreadGroups()
            , base.getThreadGroupLabels()
            , base.getForks()
            , base.getWarmupForks()
            , base.getWarmup()
            , base.getMeasurement()
            , base.getMode()
            , new WorkloadParams() // todo need to bring them from base but not exposed? Order?
            , base.getTimeUnit()
            , base.getOpsPerInvocation()
            , base.getJvm()
            , base.getJvmArgs()
            , vmInfoService.jdkVersion()
            , vmInfoService.vmName()
            , vmInfoService.vmVersion()
            , base.getJmhVersion()
            , base.getTimeout()
        );
    }
}
