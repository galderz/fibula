package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.runner.options.CommandLineOptions;

public class BootstrapMain
{
    public static void main(String[] args) throws Exception
    {
        // Read command line arguments just like JMH does
        final CommandLineOptions options = new CommandLineOptions(args);
        final BenchmarkService benchmarkService = new BenchmarkService();
        benchmarkService.run(options);
    }
}
