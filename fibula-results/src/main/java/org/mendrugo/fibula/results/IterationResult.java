package org.mendrugo.fibula.results;

public record IterationResult(
    IterationType type
    , ThroughputResult result
) {}
