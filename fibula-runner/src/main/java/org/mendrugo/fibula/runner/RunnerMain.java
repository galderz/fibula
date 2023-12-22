package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.BenchmarkResult;
import org.mendrugo.fibula.results.ThroughputResult;

import java.util.List;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    @RestClient
    ResultRestClient resultClient;

    @Inject
    @All
    List<BenchmarkSupplier> suppliers;

    @Override
    public int run(String... args)
    {
        Log.info("Running fibula.runner.RunnerMain...");
        // suppliers.forEach(BenchmarkSupplier::run);
        final BenchmarkHandler benchmarkHandler = new BenchmarkHandler();
        final Infrastructure infrastructure = new Infrastructure();
        final List<BenchmarkResult> results = suppliers.stream()
            .map(BenchmarkSupplier::get)
            .map(benchmarkFunction -> new BenchmarkCallable(benchmarkFunction, infrastructure))
            .map(benchmarkCallable -> benchmarkHandler.handle(benchmarkCallable, infrastructure))
            .toList();
        // resultProxy.send(new ThroughputResult("test", 1.0, 2.0, TimeUnit.SECONDS));
        resultClient.send(results.get(0));
        return 0;
    }
}
