package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.VmInfo;
import org.openjdk.jmh.util.Utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class VmService
{
    private final Vm vm;
    private VmInfo info;

    public VmService()
    {
        this.vm = Vm.instance();
    }

    Vm vm()
    {
        return vm;
    }

    VmInfo queryInfo() throws InterruptedException
    {
        final Process infoProcess = runInfo(vm);
        final int infoExitCode = infoProcess.waitFor();
        if (infoExitCode != 0)
        {
            throw new RuntimeException(String.format("Error in process to get VM info (exit code %d)", infoExitCode));
        }
        return info;
    }

    void setInfo(VmInfo info)
    {
        this.info = info;
    }

    private Process runInfo(Vm vm)
    {
        final List<String> baseArguments = vm.vmArguments(Utils.getCurrentJvm());
        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.COMMAND);
        arguments.add(Command.VM_INFO.toString());
        Log.debugf("Executing: %s", String.join(" ", arguments));
        try
        {
            return new ProcessBuilder(arguments).inheritIO().start();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
