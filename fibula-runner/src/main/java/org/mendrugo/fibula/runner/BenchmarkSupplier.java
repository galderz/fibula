package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.Infrastructure;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.results.BenchmarkTaskResult;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.runner.InfraControl;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface BenchmarkSupplier
{
    BiFunction<InfraControl, WorkerData, BenchmarkTaskResult> get();

    Object newInstance();
}
