package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class BootstrapLifecycle
{
    void onStart(@Observes StartupEvent ev) {
        Log.info("Bootstrap is starting...");
        final PackageTool tool = new PackageTool();
        final PackageRunner packageRunner = new PackageRunner();
        packageRunner.run(tool);
    }
}
