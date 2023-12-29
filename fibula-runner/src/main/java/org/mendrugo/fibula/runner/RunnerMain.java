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
        Log.debug("Running Fibula RunnerMain");

        final Cli cli = Cli.read(args);

        // todo add result client to handler? Make the handler a bean?
        final BenchmarkHandler benchmarkHandler = new BenchmarkHandler(cli);
        suppliers.stream()
            .map(supplier -> new BenchmarkCallable(supplier.get(), new Infrastructure(supplier.annotationParams())))
            .forEach(callable -> benchmarkHandler.runBenchmark(callable, resultClient));

        return 0;
    }
}
