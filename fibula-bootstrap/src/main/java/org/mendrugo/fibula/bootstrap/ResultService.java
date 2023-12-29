package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.JmhFormats;
import org.mendrugo.fibula.results.NativeBenchmarkParams;
import org.mendrugo.fibula.results.NativeIterationResult;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class ResultService
{
    private final List<NativeIterationResult> iterationResults = new ArrayList<>();

    private NativeOptions options;
    private int forkCounter;
    private int iterationCounter;
    private ProcessRunner processRunner;
    private OutputFormat out;

    void addIteration(NativeIterationResult result)
    {
        final BenchmarkParams benchmarkParams = getBenchmarkParams(result);
        final int forkCount = benchmarkParams.getForks();

        iterationResults.add(result);
        final int totalIterations = forkCount * benchmarkParams.getMeasurement().getCount();
        if (totalIterations == iterationResults.size())
        {
            endRun(iterationResults, benchmarkParams);
            Log.debug("Now exit the application");
            Quarkus.asyncExit();
        }

        iterationCounter++;
        if (iterationCounter == benchmarkParams.getMeasurement().getCount())
        {
            forkCounter++;
            // Run subsequent forks
            processRunner.runFork(forkCounter + 1, forkCount);
        }
    }

    void setOptions(NativeOptions options)
    {
        this.options = options;
    }

    void setProcessRunner(ProcessRunner processRunner)
    {
        this.processRunner = processRunner;
    }

    void setOutputFormat(OutputFormat out)
    {
        this.out = out;
    }

    private void endRun(List<NativeIterationResult> results, BenchmarkParams benchmarkParams)
    {
        final Collection<RunResult> runResults = runResults(results, benchmarkParams);
        final ResultFormat resultFormat = JmhFormats.resultFormat();
        resultFormat.writeOut(runResults);
    }

    private Collection<RunResult> runResults(List<NativeIterationResult> results, BenchmarkParams benchmarkParams)
    {
        final Collection<BenchmarkResult> benchmarkResults = List.of(benchmarkResult(results, benchmarkParams));
        benchmarkResults.forEach(out::endBenchmark);
        return List.of(new RunResult(benchmarkParams, benchmarkResults));
    }

    private BenchmarkResult benchmarkResult(List<NativeIterationResult> results, BenchmarkParams benchmarkParams)
    {
        final List<IterationResult> iterationResults = results.stream()
            .map(iterationResult -> Results.toIterationResult(iterationResult, benchmarkParams))
            .toList();
        return new BenchmarkResult(benchmarkParams, iterationResults);
    }

    private BenchmarkParams getBenchmarkParams(NativeIterationResult result)
    {
        return options.getBenchmarkParams(new NativeBenchmarkParams(new BenchmarkListEntry(result.benchmark())));
    }
}
