package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.RunnerArguments;

import java.util.List;
import java.util.Optional;

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
            .peek(callable -> printForkProgress(callable, cli))
            .forEach(callable -> benchmarkHandler.runBenchmark(callable, resultClient));

        return 0;
    }

    private static void printForkProgress(BenchmarkCallable callable, Cli cli)
    {
        final int cliForkCount = cli.integer(RunnerArguments.FORK_COUNT);
        final int forkIndex = cli.integer(RunnerArguments.FORK_INDEX);
        final int forkCount = callable.infrastructure.getBenchmarkParams().getMeasurementForks(Optional.of(cliForkCount));
        // todo bring back fork process logging to boostrap now that annotation params can be queried
        System.out.printf("# Fork: %d of %d%n", forkIndex, forkCount);
    }
}
