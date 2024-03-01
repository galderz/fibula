package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.Infrastructure;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.VmInfo;

import java.util.List;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    @RestClient
    IterationClient iterationClient;

    @RestClient
    VmRestClient vmClient;

    @Inject
    @All
    List<BenchmarkSupplier> suppliers;

    @Override
    public int run(String... args)
    {
        final Cli cli = Cli.read(args);
        final Command command = Command.valueOf(cli.text(RunnerArguments.COMMAND));
        switch (command)
        {
            case FORK -> runFork(cli);
            case VM_INFO -> runVmInfo();
        }

        return 0;
    }

    private void runFork(Cli cli)
    {
        final String supplierName = cli.text(RunnerArguments.SUPPLIER_NAME);

        // todo add result client to handler? Make the handler a bean?
        final Infrastructure infrastructure = new Infrastructure();
        final BenchmarkHandler benchmarkHandler = new BenchmarkHandler(cli);
        suppliers.stream()
            .filter(supplier -> supplier.getClass().getSimpleName().startsWith(supplierName))
            .map(supplier -> new BenchmarkCallable(supplier.get(), infrastructure))
            .forEach(callable -> benchmarkHandler.runBenchmark(callable, iterationClient));
    }

    private void runVmInfo()
    {
        vmClient.set(new VmInfo(
            System.getProperty("java.version")
            , System.getProperty("java.vm.name")
            , System.getProperty("java.vm.version")
        ));
    }
}
