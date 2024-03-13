package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.Infrastructure;
import org.mendrugo.fibula.results.IterationFail;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.mendrugo.fibula.results.VmInfo;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.BenchmarkException;

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
        final BenchmarkParams params = Serializables.fromBase64(cli.text(RunnerArguments.PARAMS));

        final Infrastructure infrastructure = new Infrastructure();
        suppliers.stream()
            .filter(supplier -> supplier.getClass().getSimpleName().startsWith(supplierName))
            .map(supplier -> new BenchmarkCallable(supplier.get(), infrastructure))
            .forEach(callable -> runSingle(params, callable));
    }

    private void runSingle(BenchmarkParams params, BenchmarkCallable callable)
    {
        try
        {
            runBenchmark(params, callable);
        }
        catch (BenchmarkException be)
        {
            iterationClient.notifyFail(new IterationFail(Serializables.toBase64(params), Serializables.toBase64(be)));
        }
    }

    private void runBenchmark(BenchmarkParams params, BenchmarkCallable callable)
    {
        final BenchmarkHandler benchmarkHandler = new BenchmarkHandler(iterationClient);
        try
        {
            benchmarkHandler.runBenchmark(params, callable);
        }
        catch (BenchmarkException be)
        {
            throw be;
        }
        catch (Throwable t)
        {
            throw new BenchmarkException(t);
        }
        // todo add finally to shutdown handler (e.g. executor...etc)
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
