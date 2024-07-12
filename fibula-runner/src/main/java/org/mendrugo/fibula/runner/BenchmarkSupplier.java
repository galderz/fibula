package org.mendrugo.fibula.runner;

import org.openjdk.jmh.results.BenchmarkTaskResult;
import org.openjdk.jmh.runner.InfraControl;

import java.util.function.BiFunction;

public interface BenchmarkSupplier
{
    BiFunction<InfraControl, WorkerData, BenchmarkTaskResult> get();

    Object newInstance();
}
