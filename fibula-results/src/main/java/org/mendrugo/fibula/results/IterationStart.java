package org.mendrugo.fibula.results;

// todo test with only passing iteration params (since benchmark params are included?)
public record IterationStart(
    String benchmarkParams
    , String iterationParams
    , int iteration
) {}
