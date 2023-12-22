package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.List;

@ApplicationScoped
public class BootstrapLifecycle
{
    void onStart(@Observes StartupEvent ev) {
        Log.info("Bootstrap is starting...");
        final PackageTool tool = new PackageTool();
        final ProcessRunner processRunner = new ProcessRunner();

        final PackageMode packageMode = PackageMode.JVM;
        // final PackageMode packageMode = PackageMode.NATIVE;

        final List<String> buildArguments = tool.buildArguments(packageMode);
        Log.infof("Executing: %s", String.join(" ", buildArguments));
        processRunner.runSync(new ProcessBuilder(buildArguments).inheritIO());

        final List<String> runArguments = tool.runArguments(packageMode);
        Log.infof("Executing: %s", String.join(" ", runArguments));
        processRunner.runAsync(new ProcessBuilder(runArguments).inheritIO());
    }
}
