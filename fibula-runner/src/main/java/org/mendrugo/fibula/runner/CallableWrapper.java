package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.ThroughputResult;

import java.util.concurrent.Callable;
import java.util.function.Function;

final class CallableWrapper implements Callable<ThroughputResult>
{
    private final Function<Infrastructure, ThroughputResult> benchmarkFunction;
    private final Infrastructure infra;

    public CallableWrapper(Function<Infrastructure, ThroughputResult> benchmarkFunction, Infrastructure infra)
    {
        this.benchmarkFunction = benchmarkFunction;
        this.infra = infra;
    }

    @Override
    public ThroughputResult call()
    {
        return benchmarkFunction.apply(infra);
    }
}
