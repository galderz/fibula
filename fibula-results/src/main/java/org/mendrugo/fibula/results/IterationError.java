package org.mendrugo.fibula.results;

import org.openjdk.jmh.runner.BenchmarkException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// todo create a to() conversion that takes IterationError and produces a Throwable
// todo create a to() conversion that takes IterationError and produces a BenchmarkException
public record IterationError(
    String errorMessage
    , List<Detail> details
)
{
    public static IterationError of(BenchmarkException exception)
    {
        final List<IterationError.Detail> errorDetails = Arrays.stream(exception.getSuppressed())
            .map(IterationError.Detail::toErrorDetail)
            .toList();
        return new IterationError(exception.getMessage(), errorDetails);
    }

    public static IterationError of(Throwable throwable)
    {
        return new IterationError(throwable.getMessage(), Detail.toErrorDetails(throwable));
    }

    public record Detail(
        String className
        , String message
        , StackTraceElement[] stackTrace
        , Detail cause
    )
    {
        private static List<Detail> toErrorDetails(Throwable t)
        {
            final List<IterationError.Detail> details = new ArrayList<>();
            Throwable current = t;
            while(current != null)
            {
                details.add(toErrorDetail(current));
                current = current.getCause();
            }
            return details;
        }

        private static Detail toErrorDetail(Throwable t)
        {
            if (t.getCause() != null)
            {
                final Detail cause = toErrorDetail(t.getCause());
                return new Detail(t.getClass().getName(), t.getMessage(), t.getStackTrace(), cause);
            }
            return new Detail(t.getClass().getName(), t.getMessage(), t.getStackTrace(), null);
        }
    }
}
