package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.*;
import org.mendrugo.fibula.results.ProcessExecutor.ProcessResult;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
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

    public Collection<RunResult> run(Options options) throws RunnerException
    {
        try
        {
            final Multimap<BenchmarkParams, BenchmarkResult> results = runSeparate(options);
            return resultService.endRun(results);
        }
        catch (BenchmarkException e)
        {
            throw new RunnerException("Benchmark caught the exception", e);
        }
    }

    public Multimap<BenchmarkParams, BenchmarkResult> runSeparate(Options options) throws RunnerException
    {
        final Multimap<BenchmarkParams, BenchmarkResult> results = new HashMultimap<>();
        try
        {
            Log.debugf("Virtual machine is: %s", vmService.vm());

            final VmInfo vmInfo = vmService.queryInfo();

            resultService.startRun(options);

            // Read metadata for all benchmarks
            final SortedSet<BenchmarkParams> benchmarks = findBenchmarkParams(options)
                .stream()
                .map(params -> applyVmInfo(params, vmInfo))
                .collect(Collectors.toCollection(TreeSet::new));

            List<ExternalProfiler> profilers = ProfilerFactory.getSupportedExternal(options.getProfilers());

            boolean printOut = true;
            boolean printErr = true;
            for (ExternalProfiler prof : profilers)
            {
                printOut &= prof.allowPrintOut();
                printErr &= prof.allowPrintErr();
            }

            List<ExternalProfiler> profilersRev = new ArrayList<>(profilers);
            Collections.reverse(profilersRev);

            boolean forcePrint = options.verbosity().orElse(Defaults.VERBOSITY).equalsOrHigherThan(VerboseMode.EXTRA);
            printOut = forcePrint || printOut;
            printErr = forcePrint || printErr;

            for (BenchmarkParams benchmark : benchmarks)
            {
                formatService.output().startBenchmark(benchmark);
                formatService.output().println("");

                final int forkCount = benchmark.getForks();
                for (int i = 0; i < forkCount; i++)
                {
                    final ProcessResult processResult = runFork(i + 1, benchmark, profilers, printOut, printErr);
                    if (processResult.exitCode() != 0)
                    {
                        throw new RuntimeException(String.format(
                            "Error in forked runner (exit code %d)"
                            , processResult.exitCode()
                        ));
                    }

                    final BenchmarkResult benchmarkResult = resultService.endFork(benchmark);

                    if (!profilersRev.isEmpty())
                    {
                        formatService.output().print("# Processing profiler results: ");
                        for (ExternalProfiler profiler : profilersRev)
                        {
                            formatService.output().print(profiler.getClass().getSimpleName() + " ");
                            final File stdOut = processResult.stdOut().file();
                            final File stdErr = processResult.stdErr().file();
                            final int pid = -1;
                            for (Result profR : profiler.afterTrial(benchmarkResult, pid, stdOut, stdErr))
                            {
                                benchmarkResult.addBenchmarkResult(profR);
                            }
                        }
                        formatService.output().println("");
                    }

                    results.put(benchmark, benchmarkResult);

                    processResult.stdOut().delete();
                    processResult.stdErr().delete();
                }

                formatService.output().endBenchmark(
                    new RunResult(benchmark, results.get(benchmark)).getAggregatedResult()
                );
            }
        }
        catch (InterruptedException e)
        {
            formatService.output().println("<host VM has been interrupted waiting for forked VM: " + e.getMessage() + ">");
            formatService.output().println("");
            throw new RunnerException(e);
        }
        catch (BenchmarkException e)
        {
            results.clear();
            if (options.shouldFailOnError().orElse(Defaults.FAIL_ON_ERROR))
            {
                formatService.output().println("Benchmark had encountered error, and fail on error was requested");
                throw e;
            }
        }
        finally
        {
            FileUtils.purgeTemps();
        }

        return results;
    }

    ProcessResult runFork(int forkIndex, BenchmarkParams params, List<ExternalProfiler> profilers, boolean printOut, boolean printErr)
    {
        final int forkCount = params.getForks();
        final List<String> forkArguments = forkArguments(params, profilers);
        Log.debugf("Executing: %s", String.join(" ", forkArguments));
        formatService.output().verbosePrintln("Forking using command: " + forkArguments);
        formatService.output().println("# Fork: " + forkIndex + " of " + forkCount);
        if (!profilers.isEmpty())
        {
            formatService.output().print("# Preparing profilers: ");
            for (ExternalProfiler profiler : profilers)
            {
                formatService.output().print(profiler.getClass().getSimpleName() + " ");
                profiler.beforeTrial(params);
            }
            formatService.output().println("");

            List<String> consumed = new ArrayList<>();
            if (!printOut)
            {
                consumed.add("stdout");
            }
            if (!printErr)
            {
                consumed.add("stderr");
            }

            if (!consumed.isEmpty())
            {
                formatService.output().println(String.format(
                    "# Profilers consume %s from target VM, use -v %s to copy to console"
                    , Utils.join(consumed, " and ")
                    , VerboseMode.EXTRA
                ));
            }
        }

        final ProcessExecutor processExec = new ProcessExecutor(formatService.output());
        return processExec.runSync(forkArguments, false, false);
    }

    private List<String> forkArguments(BenchmarkParams params, List<ExternalProfiler> profilers)
    {
        final List<String> javaInvokeOptions = new ArrayList<>();
        final List<String> javaOptions = new ArrayList<>();
        for (ExternalProfiler prof : profilers) {
            javaInvokeOptions.addAll(prof.addJVMInvokeOptions(params));
            javaOptions.addAll(prof.addJVMOptions(params));
        }

        final List<String> command = new ArrayList<>(javaInvokeOptions);
        final List<String> baseArguments = vmService.vm().vmArguments(params.getJvm(), javaOptions);
        command.addAll(baseArguments);

        command.add("--" + RunnerArguments.COMMAND);
        command.add(Command.FORK.toString());
        command.add("--" + RunnerArguments.SUPPLIER_NAME);
        command.add(RunnerArguments.toSupplierName(params));
        command.add("--" + RunnerArguments.PARAMS);
        command.add(Serializables.toBase64(params));
        return command;
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

    private SortedSet<BenchmarkParams> findBenchmarkParams(Options options)
    {
        final String benchmarks = readBenchmarks();
        Log.debugf("Read from benchmark list file: %n%s", benchmarks);

        final BenchmarkList benchmarkList = BenchmarkList.fromString(benchmarks);
        final List<String> includes = options.getIncludes();
        final List<String> excludes = options.getExcludes();
        Log.debugf("Find benchmarks with includes=%s and excludes=%s", includes, excludes);
        final SortedSet<BenchmarkListEntry> entries = benchmarkList.find(
            formatService.output()
            , includes
            , excludes
        );
        Log.debugf("Number of benchmarks: %d", entries.size());

        // todo would it work if do stream/map then sort?
        final TreeSet<BenchmarkParams> params = new TreeSet<>();
        for (BenchmarkListEntry entry : entries)
        {
            if (entry.getMode() == Mode.All)
            {
                Modes.nonAll().forEach(mode ->
                    params.add(getBenchmarkParams(entry.cloneWith(mode), options))
                );
            }
            else
            {
                params.add(getBenchmarkParams(entry, options));
            }
        }
        return params;
    }

    private static String readBenchmarks()
    {
        // todo move to storing the benchmarks via a recorder
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

    private static BenchmarkParams getBenchmarkParams(BenchmarkListEntry benchmark, Options jmhOptions)
    {
        final int measurementForks = JmhOptionals.<Integer>fromJmh(jmhOptions.getForkCount())
            .orElse(benchmark.getForks()
                .orElse(Defaults.MEASUREMENT_FORKS));
        final int measurementIterations = JmhOptionals.<Integer>fromJmh(jmhOptions.getMeasurementIterations())
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
