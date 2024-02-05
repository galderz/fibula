package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.UnCloseablePrintStream;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

@ApplicationScoped
public class ResultService
{
    private final SortedMap<BenchmarkParams, List<IterationResult>> iterationResults = new TreeMap<>();

    // todo consider moving these two to bean of their own
    private Optional<String> resultFile;
    private ResultFormat resultFileFormat;

    void startRun(NativeOptions nativeOptions)
    {
        resultFile = nativeOptions.resultFile();
        if (resultFile.isPresent())
        {
            resultFileFormat = nativeOptions.fileResultFormat(resultFile.get());
            try
            {
                FileUtils.touch(resultFile.get());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Can not touch the result file: " + resultFile);
            }
        }
    }

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
        final ResultFormat textResultFormat = JmhFormats.textResultFormat();
        textResultFormat.writeOut(runResults);

        if (resultFile.isPresent())
        {
            resultFileFormat.writeOut(runResults);

            // todo fix to out and move it to a bean with resultFile(Format)
            System.out.println("");
            System.out.println("Benchmark result is saved to " + resultFile.get());
        }
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
