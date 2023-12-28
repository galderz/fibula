package org.mendrugo.fibula.runner;

import org.openjdk.jmh.results.RawResults;

import java.util.function.Function;

public interface BenchmarkSupplier
{
    Function<Infrastructure, RawResults> get();

    String annotationParams();
}
