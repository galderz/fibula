package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class BootstrapLifecycle
{
    void onStart(@Observes StartupEvent ev) {
        Log.info("Bootstrap is starting...");
        final PackageTool tool = new PackageTool();
        final ProcessRunner processRunner = new ProcessRunner();

        final List<String> buildArguments = tool.arguments();
        Log.infof("Executing: %s", String.join(" ", buildArguments));
        processRunner.runSync(new ProcessBuilder(buildArguments).inheritIO());

        final List<String> runArguments = List.of(
            "java"
            , "-jar"
            , "fibula-samples/target/runner-app/quarkus-run.jar"
        );
        Log.infof("Executing: %s", String.join(" ", runArguments));
        processRunner.runAsync(new ProcessBuilder(runArguments).inheritIO());
    }
}
