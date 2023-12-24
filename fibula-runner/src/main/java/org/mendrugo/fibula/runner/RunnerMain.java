package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

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

        final Cli cli = Cli.read(args);

        // suppliers.forEach(BenchmarkSupplier::run);
        final BenchmarkHandler benchmarkHandler = new BenchmarkHandler(cli);
        final Infrastructure infrastructure = new Infrastructure();
        suppliers.stream()
            .map(BenchmarkSupplier::get)
            .map(benchmarkFunction -> new BenchmarkCallable(benchmarkFunction, infrastructure))
            .forEach(benchmarkCallable -> benchmarkHandler.runBenchmark(benchmarkCallable, resultClient, infrastructure));
        // resultProxy.send(new ThroughputResult("test", 1.0, 2.0, TimeUnit.SECONDS));
        // resultClient.send(NativeBenchmarkTaskResult.of(results.get(0)));
        return 0;
    }
}
