package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.Infrastructure;
import org.openjdk.jmh.results.RawResults;

import java.util.function.Function;

public interface BenchmarkSupplier
{
    Function<Infrastructure, RawResults> get();

    String benchmark();
}
