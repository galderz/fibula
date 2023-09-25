package org.mendrugo.fibula.runner.sample;

import org.mendrugo.fibula.results.ThroughputResult;
import org.mendrugo.fibula.runner.AbstractBenchmarkRunner;
import org.mendrugo.fibula.runner.Handler;
import org.mendrugo.fibula.runner.Infrastructure;

// todo generate automatically
public class SampleBenchmarkRunner extends AbstractBenchmarkRunner
{
    public ThroughputResult doBenchmark(Handler handler, Infrastructure infrastructure)
    {
        return handler.runIteration(new SampleBenchmark(infrastructure));
    }
}
