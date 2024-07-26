package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.IterationError;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.Utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class ResultService
{
    @Inject
    FormatService formatService;

    private final AtomicReference<List<IterationResult>> resultsRef = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<BenchmarkResultMetaData> resultMetadataRef = new AtomicReference<>();
    private final AtomicReference<BenchmarkException> exceptionRef = new AtomicReference<>();

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

    void endIteration(IterationResult result)
    {
        if (IterationType.MEASUREMENT == result.getParams().getType())
        {
            resultsRef.get().add(result);
        }
    }

    void errorIteration(String errorMessage, List<IterationError.Detail> errorDetails)
    {
        formatService.output().println("<failure>");
        formatService.output().println("");
        final BenchmarkException benchmarkException = toBenchmarkException(errorMessage, errorDetails);
        Arrays.stream(benchmarkException.getSuppressed())
            .map(Utils::throwableToString)
            .forEach(formatService.output()::println);

        formatService.output().println("");
        exceptionRef.set(benchmarkException);
    }

    void setResultMetadata(BenchmarkResultMetaData resultMetaData)
    {
        resultMetadataRef.set(resultMetaData);
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
            final Class<?> paramType = AssertionError.class == errorClass ? Object.class : String.class;
            final Constructor<?> constructor = errorClass.getDeclaredConstructor(paramType);
            final Throwable throwable = (Throwable) constructor.newInstance(errorDetail.message());
            initThrowable(errorDetail, throwable);
            return throwable;
        }
        catch (ClassNotFoundException cnfe)
        {
            final RuntimeException e = new RuntimeException(String.format("%s: %s", errorDetail.className(), errorDetail.message()));
            initThrowable(errorDetail, e);
            throw e;
            // return new Exception(String.format("%s: %s", errorDetail.className(), errorDetail.message()), errorDetail.cause());
        }
        catch (Exception e)
        {
            return new BenchmarkException(e);
        }
    }

    private static void initThrowable(IterationError.Detail errorDetail, Throwable throwable)
    {
        throwable.setStackTrace(errorDetail.stackTrace());
        if (errorDetail.cause() != null)
        {
            final Throwable cause = toSuppressedException(errorDetail.cause());
            throwable.initCause(cause);
        }
    }

    List<IterationResult> getResults()
    {
        final BenchmarkException exception = exceptionRef.getAndSet(null);
        if (exception != null)
        {
            throw exception;
        }

        return resultsRef.getAndSet(new ArrayList<>());
    }

    public BenchmarkResultMetaData getMetadata()
    {
        return resultMetadataRef.getAndSet(null);
    }

    Collection<RunResult> endRun(Multimap<BenchmarkParams, BenchmarkResult> results)
    {
        final SortedSet<RunResult> runResults = mergeRunResults(results);
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

    private static SortedSet<RunResult> mergeRunResults(Multimap<BenchmarkParams, BenchmarkResult> results) {
        SortedSet<RunResult> result = new TreeSet<>(RunResult.DEFAULT_SORT_COMPARATOR);
        for (BenchmarkParams key : results.keys()) {
            result.add(new RunResult(key, results.get(key)));
        }
        return result;
    }
}
