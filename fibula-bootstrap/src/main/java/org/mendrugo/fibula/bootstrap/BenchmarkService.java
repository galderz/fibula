package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.*;
import org.mendrugo.fibula.results.ProcessExecutor.ProcessResult;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.profile.ProfilerFactory;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.ProfilerConfig;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.TreeMultimap;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@ApplicationScoped
public class BenchmarkService
{
    @Inject
    OutputFormatService out;

    @Inject
    ResultService resultService;

    BenchmarkList benchmarkList;

    Vm vm;

    @PostConstruct
    void init()
    {
        this.vm = Vm.instance();

        final String benchmarks = readBenchmarks();
        Log.debugf("Read from benchmark list file: %n%s", benchmarks);
        this.benchmarkList = BenchmarkList.fromString(benchmarks);
    }

    public RunResult runSingle(Options options) throws RunnerException
    {
        validateProfilers(options);

        Set<BenchmarkListEntry> benchmarks = benchmarkList.find(out, options.getIncludes(), options.getExcludes());

        if (benchmarks.size() == 1)
        {
            Collection<RunResult> values = run(options);
            if (values.size() == 1)
            {
                return values.iterator().next();
            } else
            {
                throw new RunnerException("No results returned");
            }
        } else if (benchmarks.size() > 1)
        {
            throw new RunnerException("More than single benchmark are matching the options: " + benchmarks);
        } else
        {
            throw new NoBenchmarksException();
        }
    }

    public Collection<RunResult> run(Options options) throws RunnerException
    {
        return internalRun(options);
    }

    private Collection<RunResult> internalRun(Options options) throws RunnerException
    {
        validateProfilers(options);

        SortedSet<BenchmarkListEntry> benchmarks = benchmarkList.find(out, options.getIncludes(), options.getExcludes());

        overrideBenchmarkTypes(options, benchmarks);
        cloneWithAllModes(benchmarks);
        cloneWithAllParameters(options, benchmarks);

        Collection<RunResult> results = runBenchmarks(benchmarks, options);

        out.flush();
        out.close();

        return results;
    }

    private void cloneWithAllParameters(Options options, SortedSet<BenchmarkListEntry> benchmarks) throws RunnerException
    {
        List<BenchmarkListEntry> newBenchmarks = new ArrayList<>();
        for (BenchmarkListEntry br : benchmarks)
        {
            if (br.getParams().hasValue())
            {
                for (WorkloadParams p : explodeAllParams(br, options))
                {
                    newBenchmarks.add(br.cloneWith(p));
                }
            }
            else
            {
                newBenchmarks.add(br);
            }
        }
        benchmarks.clear();
        benchmarks.addAll(newBenchmarks);
    }

    private static void cloneWithAllModes(SortedSet<BenchmarkListEntry> benchmarks)
    {
        List<BenchmarkListEntry> newBenchmarks = new ArrayList<>();
        for (BenchmarkListEntry br : benchmarks)
        {
            if (br.getMode() == Mode.All)
            {
                for (Mode mode : Mode.values())
                {
                    if (mode == Mode.All) continue;
                    newBenchmarks.add(br.cloneWith(mode));
                }
            }
            else
            {
                newBenchmarks.add(br);
            }
        }

        benchmarks.clear();
        benchmarks.addAll(newBenchmarks);
    }

    private static void overrideBenchmarkTypes(Options options, SortedSet<BenchmarkListEntry> benchmarks)
    {
        if (!options.getBenchModes().isEmpty())
        {
            List<BenchmarkListEntry> newBenchmarks = new ArrayList<>();
            for (BenchmarkListEntry br : benchmarks)
            {
                for (Mode m : options.getBenchModes())
                {
                    newBenchmarks.add(br.cloneWith(m));
                }

            }

            benchmarks.clear();
            benchmarks.addAll(newBenchmarks);
        }
    }

    private Collection<RunResult> runBenchmarks(SortedSet<BenchmarkListEntry> benchmarks, Options options) throws RunnerException
    {
        out.startRun();

        final Multimap<BenchmarkParams, BenchmarkResult> results = new TreeMultimap<>();
        final List<ActionPlan> actionPlans = ActionPlans.getActionPlans(benchmarks, benchmarkList, options, out);

        try
        {
            for (ActionPlan actionPlan : actionPlans)
            {
                final Multimap<BenchmarkParams, BenchmarkResult> planResults = runSeparate(actionPlan, options);
                for (BenchmarkParams br : planResults.keys())
                {
                    results.putAll(br, planResults.get(br));
                }
            }

            final SortedSet<RunResult> runResults = mergeRunResults(results);
            out.endRun(runResults);
            return runResults;
        }
        catch (BenchmarkException be)
        {
            throw new RunnerException("Benchmark caught the exception", be);
        }
    }

    private SortedSet<RunResult> mergeRunResults(Multimap<BenchmarkParams, BenchmarkResult> results)
    {
        SortedSet<RunResult> result = new TreeSet<>(RunResult.DEFAULT_SORT_COMPARATOR);
        for (BenchmarkParams key : results.keys())
        {
            result.add(new RunResult(key, results.get(key)));
        }
        return result;
    }

    private void validateProfilers(Options options) throws RunnerException
    {
        final Set<String> profilerClasses = new HashSet<>();
        ProfilersFailedException failedException = null;
        for (ProfilerConfig p : options.getProfilers())
        {
            if (!profilerClasses.add(p.getKlass()))
            {
                throw new RunnerException("Cannot instantiate the same profiler more than once: " + p.getKlass());
            }
            try
            {
                ProfilerFactory.getProfilerOrException(p);
            }
            catch (ProfilerException e)
            {
                if (failedException == null)
                {
                    failedException = new ProfilersFailedException(e);
                }
                else
                {
                    failedException.addSuppressed(e);
                }
            }
        }
        if (failedException != null)
        {
            throw failedException;
        }
    }

    private Multimap<BenchmarkParams, BenchmarkResult> runSeparate(ActionPlan actionPlan, Options options)
    {
        final Multimap<BenchmarkParams, BenchmarkResult> results = new HashMultimap<>();
        try
        {
            Log.debugf("Virtual machine is: %s", vm);
            final VmInfo vmInfo = vm.info();
            final BenchmarkParams params = amendBenchmarkParams(ActionPlans.getParams(actionPlan), vmInfo, new Version());

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

            out.startBenchmark(params);
            out.println("");

            int forkCount = params.getForks();
            int warmupForkCount = params.getWarmupForks();
            int totalForks = warmupForkCount + forkCount;

            for (int i = 0; i < totalForks; i++)
            {
                boolean warmupFork = (i < warmupForkCount);
                final List<String> forkedString = getForkedMainCommand(params, profilers, options, actionPlan);

                // etaBeforeBenchmark();

                if (warmupFork)
                {
                    // todo dup
                    out.verbosePrintln("Warmup forking using command: " + forkedString);
                    Log.debug("Warmup forking using command: " + forkedString);
                    out.println("# Warmup Fork: " + (i + 1) + " of " + warmupForkCount);
                } else
                {
                    // todo dup
                    out.verbosePrintln("Forking using command: " + forkedString);
                    Log.debug("Forking using command: " + forkedString);
                    out.println("# Fork: " + (i + 1 - warmupForkCount) + " of " + forkCount);
                }

                if (!profilers.isEmpty())
                {
                    out.print("# Preparing profilers: ");
                    for (ExternalProfiler profiler : profilers)
                    {
                        out.print(profiler.getClass().getSimpleName() + " ");
                        profiler.beforeTrial(params);
                    }
                    out.println("");

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
                        out.println(String.format("# Profilers consume %s from target VM, use -v %s to copy to console", Utils.join(consumed, " and "), VerboseMode.EXTRA));
                    }
                }

                long startTime = System.currentTimeMillis();

                final ForkResult forkResult = doFork(forkedString);
                final List<IterationResult> result = forkResult.results;
                if (!result.isEmpty())
                {
                    final BenchmarkResultMetaData md = resultService.getMetadata();
                    if (md != null)
                    {
                        md.adjustStart(startTime);
                    }

                    final BenchmarkResult br = new BenchmarkResult(params, result, md);

                    if (!profilersRev.isEmpty())
                    {
                        out.print("# Processing profiler results: ");
                        for (ExternalProfiler profiler : profilersRev)
                        {
                            out.print(profiler.getClass().getSimpleName() + " ");
                            final File stdOut = forkResult.processResult.stdOut().file();
                            final File stdErr = forkResult.processResult.stdErr().file();
                            final int pid = -1;
                            for (Result profR : profiler.afterTrial(br, pid, stdOut, stdErr))
                            {
                                br.addBenchmarkResult(profR);
                            }
                        }
                        out.println("");
                    }

                    if (!warmupFork)
                    {
                        results.put(params, br);
                    }
                }

                // etaAfterBenchmark(params);
                out.println("");

                forkResult.close();
            }

            out.endBenchmark(new RunResult(params, results.get(params)).getAggregatedResult());
        }
        catch (BenchmarkException e)
        {
            results.clear();
            if (options.shouldFailOnError().orElse(Defaults.FAIL_ON_ERROR))
            {
                out.println("Benchmark had encountered error, and fail on error was requested");
                throw e;
            }
        }
        finally
        {
            FileUtils.purgeTemps();
        }

        return results;
    }

    ForkResult doFork(List<String> commandString)
    {
        final ProcessExecutor processExec = new ProcessExecutor(out);
        final ProcessResult processResult = processExec.runSync(commandString, false, false);

        if (processResult.exitCode() != 0)
        {
            throw new RuntimeException(String.format("Error in forked runner (exit code %d)", processResult.exitCode()));
        }

        return new ForkResult(resultService.getResults(), processResult);
    }

    private List<String> getForkedMainCommand(BenchmarkParams params, List<ExternalProfiler> profilers, Options options, ActionPlan actionPlan)
    {
        final List<String> javaInvokeOptions = new ArrayList<>();
        final List<String> javaOptions = new ArrayList<>();
        for (ExternalProfiler prof : profilers)
        {
            javaInvokeOptions.addAll(prof.addJVMInvokeOptions(params));
            javaOptions.addAll(prof.addJVMOptions(params));
        }

        final List<String> command = new ArrayList<>(javaInvokeOptions);
        final List<String> baseArguments = vm.vmArguments(params.getJvm(), params.getJvmArgs(), javaOptions);
        command.addAll(baseArguments);

        command.add("--" + RunnerArguments.OPTIONS);
        command.add(Serializables.toBase64(options));
        command.add("--" + RunnerArguments.ACTION_PLAN);
        command.add(Serializables.toBase64(actionPlan));

        return command;
    }

    private BenchmarkParams amendBenchmarkParams(BenchmarkParams params, VmInfo vmInfo, Version version)
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
            , vm.jvmArgs(params.getJvmArgs())
            , vmInfo.jdkVersion()
            , vmInfo.vmName()
            , vmInfo.vmVersion()
            , "fibula:" + version.getVersion()
            , params.getTimeout()
        );
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

    // todo Runner.explodeAllParams copy
    private List<WorkloadParams> explodeAllParams(BenchmarkListEntry br, Options options) throws RunnerException
    {
        final Map<String, String[]> benchParams = br.getParams().orElse(Collections.emptyMap());
        List<WorkloadParams> ps = new ArrayList<>();
        for (Map.Entry<String, String[]> e : benchParams.entrySet())
        {
            final String k = e.getKey();
            final String[] vals = e.getValue();
            final Collection<String> values = options.getParameter(k).orElse(Arrays.asList(vals));
            if (values.isEmpty())
            {
                throw new RunnerException("Benchmark \"" + br.getUsername() +
                    "\" defines the parameter \"" + k + "\", but no default values.\n" +
                    "Define the default values within the annotation, or provide the parameter values at runtime.");
            }
            if (ps.isEmpty())
            {
                int idx = 0;
                for (String v : values)
                {
                    WorkloadParams al = new WorkloadParams();
                    al.put(k, v, idx);
                    ps.add(al);
                    idx++;
                }
            }
            else
            {
                final List<WorkloadParams> newPs = new ArrayList<>();
                for (WorkloadParams p : ps)
                {
                    int idx = 0;
                    for (String v : values) {
                        WorkloadParams al = p.copy();
                        al.put(k, v, idx);
                        newPs.add(al);
                        idx++;
                    }
                }
                ps = newPs;
            }
        }
        return ps;
    }

    record ForkResult(
        List<IterationResult> results
        , ProcessResult processResult
    ) implements AutoCloseable
    {
        @Override
        public void close()
        {
            processResult.close();
        }
    }
}
