package org.mendrugo.fibula.jmh.it;

import org.mendrugo.fibula.MultiVmRunner;
import org.openjdk.jmh.it.RunnerFactory;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;

public interface MultiVmRunnerFactory extends RunnerFactory
{
    @Override
    default Runner createRunner(Options opts)
    {
        return new MultiVmRunner(opts);
    }
}
