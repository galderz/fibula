package org.mendrugo.fibula.results;

public record IterationFail(
    String benchmarkParams
    , String exception
) {}
