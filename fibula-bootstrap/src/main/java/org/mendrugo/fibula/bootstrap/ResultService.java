package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@ApplicationScoped
public class ResultService
{
    private final SortedMap<BenchmarkParams, List<IterationResult>> iterationResults = new TreeMap<>();

    void addIteration(IterationResult result)
    {
        final BenchmarkParams params = result.getBenchmarkParams();
        iterationResults.computeIfAbsent(params, k -> new ArrayList<>()).add(result);
    }

    void endBenchmark(BenchmarkParams params, OutputFormat out)
    {
        final Collection<BenchmarkResult> benchmarkResults = List.of(new BenchmarkResult(params, iterationResults.get(params)));
        benchmarkResults.forEach(out::endBenchmark);
    }

    void endRun()
    {
        final Collection<RunResult> runResults = getRunResults();
        final ResultFormat resultFormat = JmhFormats.resultFormat();
        resultFormat.writeOut(runResults);
    }

    private Collection<RunResult> getRunResults()
    {
        final List<RunResult> runResults = new ArrayList<>();
        for (Map.Entry<BenchmarkParams, List<IterationResult>> entry : iterationResults.entrySet())
        {
            final Collection<BenchmarkResult> benchmarkResults = List.of(new BenchmarkResult(entry.getKey(), entry.getValue()));
            final RunResult runResult = new RunResult(entry.getKey(), benchmarkResults);
            runResults.add(runResult);
        }
        return runResults;
    }
}
