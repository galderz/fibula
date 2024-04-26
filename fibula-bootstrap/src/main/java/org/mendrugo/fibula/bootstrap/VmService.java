package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.ProcessExecutor;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.VmInfo;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class VmService
{
    @Inject
    FormatService formatService;

    private Vm vm;
    private VmInfo info;

    @PostConstruct
    void init()
    {
        this.vm = Vm.instance();
    }

    Vm vm()
    {
        return vm;
    }

    VmInfo queryInfo() throws InterruptedException
    {
        final int exitValue = runInfo(vm);
        if (exitValue != 0)
        {
            throw new BenchmarkException(new IllegalStateException(String.format(
                "Error in process to get VM info (exit code %d)"
                , exitValue
            )));
        }
        return info;
    }

    void setInfo(VmInfo info)
    {
        this.info = info;
    }

    private int runInfo(Vm vm)
    {
        final List<String> baseArguments = vm.vmArguments(Utils.getCurrentJvm(), Collections.emptyList());
        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.COMMAND);
        arguments.add(Command.VM_INFO.toString());
        Log.debugf("Executing: %s", String.join(" ", arguments));

        final ProcessExecutor processExec = new ProcessExecutor(formatService.output());
        return processExec.runSync(arguments);
//        try
//        {
//            final ProcessBuilder processBuilder = new ProcessBuilder(arguments);
//            final Process process = processBuilder.start();
//
//            return process;
//        }
//        catch (IOException e)
//        {
//            throw new UncheckedIOException(e);
//        }
    }
}
