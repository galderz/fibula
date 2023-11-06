package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.ThroughputResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    @RestClient
    ResultRestClient resultProxy;

    @Inject
    @All
    List<BenchmarkSupplier> suppliers;

    @Override
    public int run(String... args)
    {
        Log.info("Running fibula.runner.RunnerMain...");
        suppliers.forEach(BenchmarkSupplier::run);
        resultProxy.send(new ThroughputResult("test", 1.0, 2.0, TimeUnit.SECONDS));
        return 0;
    }
}
