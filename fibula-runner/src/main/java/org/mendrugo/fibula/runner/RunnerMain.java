package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.IterationError;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.mendrugo.fibula.results.VmInfo;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.BenchmarkException;

import java.util.Arrays;
import java.util.Collections;
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
        final BenchmarkParams benchmarkParams = Serializables.fromBase64(cli.text(RunnerArguments.PARAMS));

        final List<BenchmarkSupplier> matchingSuppliers = suppliers.stream()
            .filter(supplier -> RunnerArguments.isSupplier(supplierName, supplier.getClass()))
            .toList();

        if (matchingSuppliers.isEmpty())
        {
            final List<String> allSupplierNames = suppliers.stream()
                .map(supplier -> RunnerArguments.toSupplierName(supplier.getClass()))
                .toList();

            final String errorMsg = String.format(
                "No benchmarks found with supplier starting with %s in %s"
                , supplierName
                , allSupplierNames
            );

            final BenchmarkException e = new BenchmarkException(errorMsg, Collections.emptyList());
            iterationClient.notifyError(createIterationError(e));
            return;
        }

        matchingSuppliers.forEach(benchmark -> runSingle(benchmark, benchmarkParams));
    }

    private void runSingle(BenchmarkSupplier benchmark, BenchmarkParams benchmarkParams)
    {
        try
        {
            runBenchmark(benchmark, benchmarkParams);
        }
        catch (BenchmarkException be)
        {
            iterationClient.notifyError(createIterationError(be));
        }
    }

    private static IterationError createIterationError(BenchmarkException exception)
    {
        final List<IterationError.Detail> errorDetails = Arrays.stream(exception.getSuppressed())
            .map(RunnerMain::toErrorDetail)
            .toList();
        return new IterationError(exception.getMessage(), errorDetails);
    }

    private static IterationError.Detail toErrorDetail(Throwable t)
    {
        if (t.getCause() != null)
        {
            final IterationError.Detail cause = toErrorDetail(t.getCause());
            return new IterationError.Detail(t.getClass().getName(), t.getMessage(), t.getStackTrace(), cause);
        }
        return new IterationError.Detail(t.getClass().getName(), t.getMessage(), t.getStackTrace(), null);
    }

    private void runBenchmark(BenchmarkSupplier benchmark, BenchmarkParams benchmarkParams) {
        final BenchmarkHandler benchmarkHandler = new BenchmarkHandler(iterationClient, benchmarkParams);
        try
        {
            benchmarkHandler.runBenchmark(benchmark);
        }
        catch (BenchmarkException be)
        {
            throw be;
        }
        catch (Throwable t)
        {
            throw new BenchmarkException(t);
        }
        finally
        {
            benchmarkHandler.shutdown();
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
