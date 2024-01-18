package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.RunnerArguments;

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
        Log.debug("Running forked runner");

        final Cli cli = Cli.read(args);
        final String supplierName = cli.text(RunnerArguments.SUPPLIER_NAME);

        // todo add result client to handler? Make the handler a bean?
        final Infrastructure infrastructure = new Infrastructure();
        final BenchmarkHandler benchmarkHandler = new BenchmarkHandler(cli);
        suppliers.stream()
            .filter(supplier -> supplier.getClass().getSimpleName().startsWith(supplierName))
            .map(supplier -> new BenchmarkCallable(supplier.get(), infrastructure))
            .forEach(callable -> benchmarkHandler.runBenchmark(callable, resultClient));

        return 0;
    }
}
