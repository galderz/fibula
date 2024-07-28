package org.mendrugo.fibula.runner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.Command;
import org.mendrugo.fibula.results.IterationError;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.mendrugo.fibula.results.VmInfo;
import org.openjdk.jmh.runner.ActionPlan;
import org.openjdk.jmh.runner.DualRunner;
import org.openjdk.jmh.runner.options.Options;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    @RestClient
    IterationClient iterationClient;

    @Inject
    OutputFormatDelegate outputFormat;

    @RestClient
    VmRestClient vmClient;

    @Override
    public int run(String... args) throws Exception
    {
        try
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
        catch (Throwable t)
        {
            iterationClient.notifyError(IterationError.of(t));
            throw t;
        }
    }

    private void runFork(Cli cli)
    {
        final Options options = Serializables.fromBase64(cli.text(RunnerArguments.OPTIONS));
        final ActionPlan actionPlan = Serializables.fromBase64(cli.text(RunnerArguments.ACTION_PLAN));
        final DualRunner runner = new DualRunner(options, outputFormat, iterationClient);
        runner.run(actionPlan);
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
