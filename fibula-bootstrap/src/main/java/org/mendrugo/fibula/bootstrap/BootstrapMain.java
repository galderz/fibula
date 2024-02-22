package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.JmhFormats;
import org.mendrugo.fibula.results.JmhOptionals;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
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

        final Vm vm = Vm.instance();
        Log.debugf("Virtual machine is: %s", vm);

        final OutputFormat out = JmhFormats.outputFormat();
        final ProcessRunner processRunner = new ProcessRunner(out);

        final Process infoProcess = processRunner.runInfo(vm);
        final int infoExitCode = infoProcess.waitFor();
        if (infoExitCode != 0)
        {
            throw new RuntimeException(String.format(
                "Error in process to get VM info (exit code %d)"
                , infoExitCode
            ));
        }

        resultService.startRun(jmhOptions);

        // Read metadata for all benchmarks
        final SortedSet<BenchmarkParams> benchmarks = findBenchmarkParams(out, jmhOptions)
            .stream()
            .map(params -> applyVmInfo(params, vm))
            .collect(Collectors.toCollection(TreeSet::new));

        for (BenchmarkParams benchmark : benchmarks)
        {
            out.startBenchmark(benchmark);
            out.println("");

            final int forkCount = benchmark.getMeasurement().getCount();
            for (int i = 0; i < forkCount; i++)
            {
                final Process process = processRunner.runFork(i + 1, benchmark, vm);
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

    private BenchmarkParams applyVmInfo(BenchmarkParams params, Vm vm)
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
            , vm.executablePath(params.getJvm())
            , params.getJvmArgs()
            , vmInfoService.jdkVersion()
            , vmInfoService.vmName()
            , vmInfoService.vmVersion()
            , params.getJmhVersion()
            , params.getTimeout()
        );
    }

    private SortedSet<BenchmarkParams> findBenchmarkParams(OutputFormat out, Options jmhOptions)
    {
        final String benchmarks = readBenchmarks();
        Log.debugf("Read from benchmark list file: %n%s", benchmarks);

        final BenchmarkList benchmarkList = BenchmarkList.fromString(benchmarks);
        final SortedSet<BenchmarkListEntry> entries = benchmarkList.find(out, jmhOptions.getIncludes(), jmhOptions.getExcludes());
        Log.debugf("Number of benchmarks: %d", entries.size());

        // todo would it work if do stream/map then sort?
        final TreeSet<BenchmarkParams> params = new TreeSet<>();
        for (BenchmarkListEntry entry : entries)
        {
            params.add(getBenchmarkParams(entry, jmhOptions));
        }
        return params;
    }

    private String readBenchmarks()
    {
        final File resourceDir = Path.of("target", "classes").toFile();
        final FileSystemDestination destination = new FileSystemDestination(resourceDir, null);
        try (InputStream stream = destination.getResource(BenchmarkList.BENCHMARK_LIST.substring(1)))
        {
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                final Collection<String> lines = FileUtils.readAllLines(reader);
                return String.join(System.lineSeparator(), lines);
            }
        }
        catch (IOException e)
        {
            Log.debug("Unable to read benchmark list", e);
        }
        catch (UnsupportedOperationException e)
        {
            final String msg = "Unable to read the existing benchmark list.";
            Log.debug(msg, e);
            destination.printError(msg, e);
        }
        return "";
    }

    private BenchmarkParams getBenchmarkParams(BenchmarkListEntry benchmark, Options jmhOptions)
    {
        final int measurementForks = JmhOptionals.<Integer>fromJmh(jmhOptions.getForkCount())
            .orElse(benchmark.getForks()
                .orElse(Defaults.MEASUREMENT_FORKS));
        final int measurementIterations = JmhOptionals.<Integer>fromJmh(jmhOptions.getWarmupIterations())
            .orElse(benchmark.getMeasurementIterations()
                .orElse(benchmark.getMode() == Mode.SingleShotTime
                    ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT
                    : Defaults.MEASUREMENT_ITERATIONS)
            );
        final TimeValue measurementTime = JmhOptionals.<TimeValue>fromJmh(jmhOptions.getMeasurementTime())
            .orElse(benchmark.getMeasurementTime()
                .orElse(benchmark.getMode() == Mode.SingleShotTime
                    ? TimeValue.NONE
                    : Defaults.MEASUREMENT_TIME)
            );

        final TimeUnit outputTimeUnit = JmhOptionals.<TimeUnit>fromJmh(jmhOptions.getTimeUnit())
            .orElse(benchmark.getTimeUnit()
                .orElse(Defaults.OUTPUT_TIMEUNIT)
            );

        final int warmupForks = JmhOptionals.<Integer>fromJmh(jmhOptions.getWarmupForkCount())
            .orElse(benchmark.getForks()
                .orElse(Defaults.WARMUP_FORKS));
        final int warmupIterations = JmhOptionals.<Integer>fromJmh(jmhOptions.getWarmupIterations())
            .orElse(benchmark.getWarmupIterations()
                .orElse(benchmark.getMode() == Mode.SingleShotTime
                    ? Defaults.WARMUP_ITERATIONS_SINGLESHOT
                    : Defaults.WARMUP_ITERATIONS)
            );
        final TimeValue warmupTime = JmhOptionals.<TimeValue>fromJmh(jmhOptions.getWarmupTime())
            .orElse(benchmark.getWarmupTime()
                .orElse(benchmark.getMode() == Mode.SingleShotTime
                    ? TimeValue.NONE
                    : Defaults.WARMUP_TIME)
            );

        final String jvm = JmhOptionals.<String>fromJmh(jmhOptions.getJvm())
            .orElse(benchmark.getJvm()
                .orElse(Utils.getCurrentJvm()));

        final IterationParams warmup = new IterationParams(
            IterationType.WARMUP
            , warmupIterations
            , warmupTime
            , Defaults.WARMUP_BATCHSIZE
        );

        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , measurementIterations
            , measurementTime
            , Defaults.MEASUREMENT_BATCHSIZE
        );
        final WorkloadParams params = new WorkloadParams();

        // Null values fixed at runtime based on vm running fork
        return new BenchmarkParams(
            benchmark.getUsername()
            , benchmark.generatedTarget()
            , true
            , 1
            , new int[]{1}
            , Collections.emptyList()
            , measurementForks
            , warmupForks
            , warmup
            , measurement
            , benchmark.getMode()
            , params
            , outputTimeUnit
            , 1
            , jvm
            , new ArrayList<>()
            , null
            , null
            , null
            , "fibula-999"
            , TimeValue.minutes(10)
        );
    }
}
