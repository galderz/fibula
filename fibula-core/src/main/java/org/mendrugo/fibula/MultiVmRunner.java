package org.mendrugo.fibula;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;

public final class MultiVmRunner extends Runner
{
    public MultiVmRunner(Options options)
    {
        super(options, new MultiVmOutputFormat(options));
    }
}
