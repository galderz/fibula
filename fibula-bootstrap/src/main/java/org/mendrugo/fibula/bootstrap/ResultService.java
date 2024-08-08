package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.IterationError;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.util.Utils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class ResultService
{
    @Inject
    OutputFormatService out;

    private final AtomicReference<List<IterationResult>> resultsRef = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<BenchmarkResultMetaData> resultMetadataRef = new AtomicReference<>();
    private final AtomicReference<BenchmarkException> exceptionRef = new AtomicReference<>();

    void endIteration(IterationResult result)
    {
        if (IterationType.MEASUREMENT == result.getParams().getType())
        {
            resultsRef.get().add(result);
        }
    }

    void errorIteration(String errorMessage, List<IterationError.Detail> errorDetails)
    {
        out.println("<failure>");
        out.println("");
        final BenchmarkException benchmarkException = toBenchmarkException(errorMessage, errorDetails);
        Arrays.stream(benchmarkException.getSuppressed())
            .map(Utils::throwableToString)
            .forEach(out::println);

        out.println("");
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
            return e;
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
}
