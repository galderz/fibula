package org.mendrugo.fibula.bootstrap;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.openjdk.jmh.runner.options.CommandLineOptions;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain implements QuarkusApplication
{
    @Inject
    BenchmarkService benchmarkService;

    @Override
    public int run(String... args) throws Exception
    {
        // Read command line arguments just like JMH does
        final CommandLineOptions options = new CommandLineOptions(args);
        benchmarkService.run(options);
        return 0;
    }
}
