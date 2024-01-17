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
import java.util.List;

@ApplicationScoped
public class ResultService
{
    private final List<IterationResult> iterationResults = new ArrayList<>();

    void addIteration(IterationResult result)
    {
        iterationResults.add(result);
    }

    void endRun(OutputFormat out)
    {
        final BenchmarkParams benchmarkParams = iterationResults.iterator().next().getBenchmarkParams();
        final Collection<BenchmarkResult> benchmarkResults = List.of(new BenchmarkResult(benchmarkParams, iterationResults));
        benchmarkResults.forEach(out::endBenchmark);
        final Collection<RunResult> runResults = List.of(new RunResult(benchmarkParams, benchmarkResults));

        final ResultFormat resultFormat = JmhFormats.resultFormat();
        resultFormat.writeOut(runResults);
    }
}
