package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
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

    private NativeOptions options;
    private int forkCounter;
    private int iterationCounter;
    private ProcessRunner processRunner;
    private OutputFormat out;

    void addIteration(IterationResult result)
    {
        final BenchmarkParams benchmarkParams = result.getBenchmarkParams();
        final int forkCount = benchmarkParams.getForks();

        iterationResults.add(result);
        final int totalIterations = forkCount * benchmarkParams.getMeasurement().getCount();
        if (totalIterations == iterationResults.size())
        {
            endRun();
            Log.debug("Now exit the application");
            Quarkus.asyncExit();
        }

        iterationCounter++;
        if (iterationCounter == benchmarkParams.getMeasurement().getCount())
        {
            forkCounter++;
            // Run subsequent forks
            processRunner.runFork(forkCounter + 1, benchmarkParams);
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

    private void endRun()
    {
        final BenchmarkParams benchmarkParams = iterationResults.iterator().next().getBenchmarkParams();
        final Collection<BenchmarkResult> benchmarkResults = List.of(new BenchmarkResult(benchmarkParams, iterationResults));
        benchmarkResults.forEach(out::endBenchmark);
        final Collection<RunResult> runResults = List.of(new RunResult(benchmarkParams, benchmarkResults));

        final ResultFormat resultFormat = JmhFormats.resultFormat();
        resultFormat.writeOut(runResults);
    }

//    private BenchmarkParams getBenchmarkParams(NativeIterationResult result)
//    {
//        return options.getBenchmarkParams(new NativeBenchmarkParams(new BenchmarkListEntry(result.benchmark())));
//    }
}
