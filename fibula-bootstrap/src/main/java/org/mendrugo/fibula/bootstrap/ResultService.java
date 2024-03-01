package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.FileUtils;

import java.io.IOException;
import java.util.*;

@ApplicationScoped
public class ResultService
{
    @Inject
    FormatService formatService;

    private final SortedMap<BenchmarkParams, List<IterationResult>> iterationResults = new TreeMap<>();

    // todo consider moving these two to bean of their own
    private Optional<String> resultFile;
    private ResultFormat resultFileFormat;

    void startRun(Options jmhOptions)
    {
        resultFile = resultFile(jmhOptions);
        if (resultFile.isPresent())
        {
            resultFileFormat = ResultFormatFactory.getInstance(
                jmhOptions.getResultFormat().orElse(Defaults.RESULT_FORMAT)
                , resultFile.get()
            );
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

    private Optional<String> resultFile(Options jmhOptions)
    {
        if (jmhOptions.getResult().hasValue() || jmhOptions.getResultFormat().hasValue()) {
            final String format = jmhOptions.getResultFormat()
                .orElse(Defaults.RESULT_FORMAT).toString().toLowerCase();
            return Optional.of(jmhOptions.getResult().orElse(Defaults.RESULT_FILE_PREFIX + "." + format));
        }

        return Optional.empty();
    }

    public void startIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, int iteration)
    {
        formatService.output().iteration(benchmarkParams, iterationParams, iteration);
    }

    void endIteration(int iteration, IterationResult result)
    {
        final BenchmarkParams benchmarkParams = result.getBenchmarkParams();
        final IterationParams iterationParams = result.getParams();
        formatService.output().iterationResult(benchmarkParams, iterationParams, iteration, result);
        if (IterationType.MEASUREMENT == iterationParams.getType())
        {
            iterationResults.computeIfAbsent(benchmarkParams, k -> new ArrayList<>()).add(result);
        }
    }

    void endBenchmark(BenchmarkParams params)
    {
        final Collection<BenchmarkResult> benchmarkResults = List.of(new BenchmarkResult(params, iterationResults.get(params)));
        benchmarkResults.forEach(formatService.output()::endBenchmark);
    }

    Collection<RunResult> endRun()
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

        return runResults;
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
