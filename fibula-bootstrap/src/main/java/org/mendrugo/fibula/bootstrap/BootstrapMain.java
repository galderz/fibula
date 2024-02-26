package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain implements QuarkusApplication
{
    @Inject
    BenchmarkService benchmarkService;

    @Override
    public int run(String... args) throws Exception
    {
        Log.debug("Running bootstrap");
        // Read command line arguments just like JMH does
        benchmarkService.run();
        return 0;
    }
}
