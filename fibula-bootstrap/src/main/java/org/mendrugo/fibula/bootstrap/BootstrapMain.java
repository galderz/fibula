package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

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

        // todo read command line args and extract package mode
        final PackageMode packageMode = PackageMode.JVM;
        resultService.setPackageMode(packageMode);

        final PackageTool tool = new PackageTool();
        final ProcessRunner processRunner = new ProcessRunner();

        final List<String> buildArguments = tool.buildArguments(packageMode);
        Log.infof("Executing: %s", String.join(" ", buildArguments));
        processRunner.runSync(new ProcessBuilder(buildArguments).inheritIO());

        final List<String> runArguments = tool.runArguments(packageMode);
        Log.infof("Executing: %s", String.join(" ", runArguments));
        System.out.println("# Fork: 1 of 5");
        processRunner.runAsync(new ProcessBuilder(runArguments).inheritIO());

        // todo I need bootstrap lifecycle?
        // e.g. I can fire the build and the first invocation/fork
        //      then the result service can fire more if neeeded
        //      and eventually exit
        Quarkus.waitForExit();
        return 0;  // TODO: Customise this generated block
    }
}
