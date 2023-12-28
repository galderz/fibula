package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;

import java.util.List;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain implements QuarkusApplication
{
    @Inject
    ResultService resultService;

    @Override
    public int run(String... args) throws Exception
    {
        System.out.println("Running bootstrap main...");

        // Read command line arguments just like JMH does
        final Options jmhOptions = new CommandLineOptions(args);
        final NativeOptions options = new NativeOptions(jmhOptions);
        resultService.setOptions(options);

        final ProcessRunner processRunner = new ProcessRunner(options);
        resultService.setProcessRunner(processRunner);

        // Build the runner and run the first fork
        processRunner.runBuild();
        processRunner.runFork(0);

        Quarkus.waitForExit();
        return 0;
    }
}
