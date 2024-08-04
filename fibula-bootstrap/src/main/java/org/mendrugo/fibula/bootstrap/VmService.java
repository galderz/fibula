package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.ProcessExecutor;
import org.mendrugo.fibula.results.ProcessExecutor.ProcessResult;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.VmInfo;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class VmService
{
    @Inject
    OutputFormatService out;

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
        final List<String> baseArguments = vm.vmArguments(Utils.getCurrentJvm(), Collections.emptyList(), Collections.emptyList());
        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.COMMAND);
        arguments.add(Command.VM_INFO.toString());
        Log.debugf("Executing: %s", String.join(" ", arguments));

        final ProcessExecutor processExec = new ProcessExecutor(out);
        try (ProcessResult result = processExec.runSync(arguments, false, false))
        {
            return result.exitCode();
        }
    }
}
