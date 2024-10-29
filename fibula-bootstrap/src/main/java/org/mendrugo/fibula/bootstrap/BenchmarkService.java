package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;

import java.util.Collection;

public class BenchmarkService
{
    public Collection<RunResult> run(Options options) throws RunnerException
    {
        return new Runner(options, new BootstrapOutputFormat(options)).run();
    }

    public RunResult runSingle(Options options) throws RunnerException
    {
        return new Runner(options, new BootstrapOutputFormat(options)).runSingle();
    }
}
