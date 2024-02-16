package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.util.Utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

final class ProcessRunner
{
    private final OutputFormat out;

    ProcessRunner(OutputFormat out)
    {
        this.out = out;
    }


    Process runFork(int forkIndex, BenchmarkParams params, VmInvoker vmInvoker)
    {
        final int forkCount = params.getMeasurement().getCount();
        final List<String> forkArguments = forkArguments(params, vmInvoker);
        Log.debugf("Executing: %s", String.join(" ", forkArguments));
        out.println("# Fork: " + forkIndex + " of " + forkCount);
        return runAsync(new ProcessBuilder(forkArguments).inheritIO());
    }

    Process runInfo(VmInvoker vmInvoker)
    {
        final List<String> baseArguments = vmInvoker.vmArguments(Utils.getCurrentJvm());
        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.COMMAND);
        arguments.add(Command.VM_INFO.toString());
        Log.debugf("Executing: %s", String.join(" ", arguments));
        return runAsync(new ProcessBuilder(arguments).inheritIO());
    }

    private Process runAsync(ProcessBuilder processBuilder)
    {
        try
        {
            return processBuilder.start();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> forkArguments(BenchmarkParams params, VmInvoker vmInvoker)
    {
        final List<String> baseArguments = vmInvoker.vmArguments(params.getJvm());
        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.COMMAND);
        arguments.add(Command.FORK.toString());
        arguments.add("--" + RunnerArguments.SUPPLIER_NAME);
        arguments.add(params.generatedBenchmark().replace(".", "_") + "_Supplier");
        arguments.add("--" + RunnerArguments.PARAMS);
        arguments.add(Serializables.toBase64(params));
        return arguments;
    }
}
