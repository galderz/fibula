package org.mendrugo.fibula.runner;

import org.openjdk.jmh.results.RawResults;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class BenchmarkCallable implements Callable<RawResults>
{
    final Function<Infrastructure, RawResults> function;
    final Infrastructure infrastructure;

    public BenchmarkCallable(Function<Infrastructure, RawResults> function, Infrastructure infrastructure) {
        this.function = function;
        this.infrastructure = infrastructure;
    }

    @Override
    public RawResults call()
    {
        return function.apply(infrastructure);
    }
}
