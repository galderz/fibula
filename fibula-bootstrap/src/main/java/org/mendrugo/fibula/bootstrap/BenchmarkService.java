package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.*;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApplicationScoped
public class BenchmarkService
{
    @Inject
    FormatService formatService;

    @Inject
    ResultService resultService;

    @Inject
    VmService vmService;

    // todo remove the thrown exception
    public Collection<RunResult> run(Options jmhOptions) throws InterruptedException
    {
        Log.debugf("Virtual machine is: %s", vmService.vm());

        final VmInfo vmInfo = vmService.queryInfo();

        resultService.startRun(jmhOptions);

        // Read metadata for all benchmarks
        final SortedSet<BenchmarkParams> benchmarks = findBenchmarkParams(jmhOptions)
            .stream()
            .map(params -> applyVmInfo(params, vmInfo))
            .collect(Collectors.toCollection(TreeSet::new));

        for (BenchmarkParams benchmark : benchmarks)
        {
            formatService.output().startBenchmark(benchmark);
            formatService.output().println("");

            final int forkCount = benchmark.getMeasurement().getCount();
            for (int i = 0; i < forkCount; i++)
            {
                final Process process = runFork(i + 1, benchmark);
                final int exitCode = process.waitFor();
                if (exitCode != 0)
                {
                    throw new RuntimeException(String.format(
                        "Error in forked runner (exit code %d)"
                        , exitCode
                    ));
                }
            }

            resultService.endBenchmark(benchmark);
        }

        return resultService.endRun();
    }

    Process runFork(int forkIndex, BenchmarkParams params)
    {
        final int forkCount = params.getMeasurement().getCount();
        final List<String> forkArguments = forkArguments(params);
        Log.debugf("Executing: %s", String.join(" ", forkArguments));
        formatService.output().println("# Fork: " + forkIndex + " of " + forkCount);
        try
        {
            return new ProcessBuilder(forkArguments).inheritIO().start();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> forkArguments(BenchmarkParams params)
    {
        final List<String> baseArguments = vmService.vm().vmArguments(params.getJvm());
        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.COMMAND);
        arguments.add(Command.FORK.toString());
        arguments.add("--" + RunnerArguments.SUPPLIER_NAME);
        arguments.add(params.generatedBenchmark().replace(".", "_") + "_Supplier");
        arguments.add("--" + RunnerArguments.PARAMS);
        arguments.add(Serializables.toBase64(params));
        return arguments;
    }

    private BenchmarkParams applyVmInfo(BenchmarkParams params, VmInfo vmInfo)
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
            , vmService.vm().executablePath(params.getJvm())
            , params.getJvmArgs()
            , vmInfo.jdkVersion()
            , vmInfo.vmName()
            , vmInfo.vmVersion()
            , params.getJmhVersion()
            , params.getTimeout()
        );
    }

    private SortedSet<BenchmarkParams> findBenchmarkParams(Options jmhOptions)
    {
        final String benchmarks = readBenchmarks();
        Log.debugf("Read from benchmark list file: %n%s", benchmarks);

        final BenchmarkList benchmarkList = BenchmarkList.fromString(benchmarks);
        final SortedSet<BenchmarkListEntry> entries = benchmarkList.find(
            formatService.output()
            , jmhOptions.getIncludes()
            , jmhOptions.getExcludes()
        );
        Log.debugf("Number of benchmarks: %d", entries.size());

        // todo would it work if do stream/map then sort?
        final TreeSet<BenchmarkParams> params = new TreeSet<>();
        for (BenchmarkListEntry entry : entries)
        {
            params.add(getBenchmarkParams(entry, jmhOptions));
        }
        return params;
    }

    private static String readBenchmarks()
    {
        // todo move to storing the benchmarks via a recorder and avoid the root issue
        final Path root = Path.of(System.getProperty("fibula.root", "."));
        final File resourceDir = root.resolve(Path.of("target", "classes")).toFile();
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

    private static BenchmarkParams getBenchmarkParams(BenchmarkListEntry benchmark, Options jmhOptions)
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
