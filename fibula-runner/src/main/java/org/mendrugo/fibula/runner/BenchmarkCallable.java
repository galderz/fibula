package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.ThroughputResult;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class BenchmarkCallable implements Callable<ThroughputResult>
{
    final Function<Infrastructure, ThroughputResult> function;
    final Infrastructure infrastructure;

    public BenchmarkCallable(Function<Infrastructure, ThroughputResult> function, Infrastructure infrastructure) {
        this.function = function;
        this.infrastructure = infrastructure;
    }

    @Override
    public ThroughputResult call()
    {
        return function.apply(infrastructure);
    }
}
