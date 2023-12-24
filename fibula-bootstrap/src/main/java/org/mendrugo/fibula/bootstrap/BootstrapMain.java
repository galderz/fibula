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

        final PackageTool tool = new PackageTool();
        final ProcessRunner processRunner = new ProcessRunner();

        final List<String> buildArguments = tool.buildArguments(options.getPackageMode());
        Log.infof("Executing: %s", String.join(" ", buildArguments));
        processRunner.runSync(new ProcessBuilder(buildArguments).inheritIO());

        // todo forward command line arguments onto the runner process
        //      so that it can extract iteration count...etc.
        final List<String> runArguments = tool.runArguments(options.getPackageMode(), options.getRunnerArguments());
        Log.infof("Executing: %s", String.join(" ", runArguments));
        // System.out.println("# Fork: 1 of 5");
        processRunner.runAsync(new ProcessBuilder(runArguments).inheritIO());

        // todo I need bootstrap lifecycle?
        // e.g. I can fire the build and the first invocation/fork
        //      then the result service can fire more if neeeded
        //      and eventually exit
        Quarkus.waitForExit();
        return 0;  // TODO: Customise this generated block
    }
}
