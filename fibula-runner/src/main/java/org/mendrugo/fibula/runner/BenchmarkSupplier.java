package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.ThroughputResult;

import java.util.function.Function;

public interface BenchmarkSupplier
{
    Function<Infrastructure, ThroughputResult> get();
}
