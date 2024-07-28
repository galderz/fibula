package org.mendrugo.fibula.results;

public record IterationEnd(
    String benchmarkParams
    , String iterationParams
    , int iteration
    , String iterationResult
) {}
