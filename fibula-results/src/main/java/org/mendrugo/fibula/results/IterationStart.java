package org.mendrugo.fibula.results;

public record IterationStart(
    String benchmarkParams // todo do I need this? doesn't iteration params include benchparams?
    , String iterationParams
    , int iteration
) {}
