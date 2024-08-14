package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.BootstrapRunner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;

import java.util.Collection;

@ApplicationScoped
public class BenchmarkService
{
    public Collection<RunResult> run(Options options) throws RunnerException
    {
        return new BootstrapRunner(options).run();
    }

    public RunResult runSingle(Options options) throws RunnerException
    {
        return new BootstrapRunner(options).runSingle();
    }
}
