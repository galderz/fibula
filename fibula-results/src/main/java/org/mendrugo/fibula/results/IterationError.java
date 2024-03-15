package org.mendrugo.fibula.results;

import java.util.List;

public record IterationError(
    String benchmarkParams
    , String errorMessage
    , List<Detail> details
)
{
    public record Detail(
        String className
        , String message
        , StackTraceElement[] stackTrace
    ) {}
}
