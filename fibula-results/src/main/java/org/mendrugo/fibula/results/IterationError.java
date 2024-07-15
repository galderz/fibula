package org.mendrugo.fibula.results;

import java.util.List;

public record IterationError(
    String errorMessage
    , List<Detail> details
)
{
    public record Detail(
        String className
        , String message
        , StackTraceElement[] stackTrace
        , Detail cause
    ) {}
}
