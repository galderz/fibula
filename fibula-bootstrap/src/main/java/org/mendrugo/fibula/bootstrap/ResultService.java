package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.IterationError;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

@ApplicationScoped
public class ResultService
{
    @Inject
    FormatService formatService;

    private final SortedMap<BenchmarkParams, Either<BenchmarkException, List<IterationResult>>> iterationResults = new TreeMap<>();

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

    void startIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, int iteration)
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
            iterationResults
                .computeIfAbsent(benchmarkParams, k -> Either.right(new ArrayList<>()))
                .right()
                .add(result);
        }
    }

    void errorIteration(BenchmarkParams params, String errorMessage, List<IterationError.Detail> errorDetails)
    {
        formatService.output().println("<failure>");
        formatService.output().println("");
        final BenchmarkException benchmarkException = toBenchmarkException(errorMessage, errorDetails);
        Arrays.stream(benchmarkException.getSuppressed())
            .map(Utils::throwableToString)
            .forEach(formatService.output()::println);

        formatService.output().println("");
        iterationResults.put(params, Either.left(benchmarkException));
    }

    private static BenchmarkException toBenchmarkException(String errorMessage, List<IterationError.Detail> errorDetails)
    {
        final List<Throwable> suppressedExceptions = errorDetails.stream()
            .map(ResultService::toSuppressedException)
            .toList();

        return new BenchmarkException(errorMessage, suppressedExceptions);
    }

    private static Throwable toSuppressedException(IterationError.Detail errorDetail)
    {
        try
        {
            final Class<?> errorClass = Class.forName(errorDetail.className());
            final Constructor<?> constructor = errorClass.getDeclaredConstructor(String.class);
            final Throwable throwable = (Throwable) constructor.newInstance(errorDetail.message());
            throwable.setStackTrace(errorDetail.stackTrace());
            if (errorDetail.cause() != null)
            {
                final Throwable cause = toSuppressedException(errorDetail.cause());
                throwable.initCause(cause);
            }
            return throwable;
        }
        catch (Exception e)
        {
            throw new BenchmarkException(e);
        }
    }

    void endBenchmark(BenchmarkParams params, Options options)
    {
        final Either<BenchmarkException, List<IterationResult>> either = iterationResults.get(params);
        switch (either)
        {
            case Either.Right<BenchmarkException, List<IterationResult>> right ->
            {
                final Collection<BenchmarkResult> benchmarkResults = List.of(new BenchmarkResult(params, right.right()));
                benchmarkResults.forEach(formatService.output()::endBenchmark);
            }
            case Either.Left<BenchmarkException, List<IterationResult>> left ->
            {
                if (options.shouldFailOnError().orElse(Defaults.FAIL_ON_ERROR))
                {
                    throw left.left();
                }
            }
        }
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
        for (Map.Entry<BenchmarkParams, Either<BenchmarkException, List<IterationResult>>> entry : iterationResults.entrySet())
        {
            final Either<BenchmarkException, List<IterationResult>> value = entry.getValue();
            switch (value)
            {
                case Either.Right<BenchmarkException, List<IterationResult>> right ->
                {
                    final Collection<BenchmarkResult> benchmarkResults = List.of(new BenchmarkResult(entry.getKey(), right.right()));
                    final RunResult runResult = new RunResult(entry.getKey(), benchmarkResults);
                    runResults.add(runResult);
                }
                default -> {}
            }
        }
        iterationResults.clear();
        return runResults;
    }
}
