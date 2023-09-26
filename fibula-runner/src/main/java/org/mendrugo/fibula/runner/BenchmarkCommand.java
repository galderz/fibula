package org.mendrugo.fibula.runner;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.ThroughputResult;
import org.mendrugo.fibula.runner.client.ResultProxy;
import picocli.CommandLine;

import java.util.function.Function;

@TopCommand // todo needed?
@CommandLine.Command
public class BenchmarkCommand implements Runnable
{
    @CommandLine.Option(names = {"-n", "--name"}, description = "Who will we greet?", defaultValue = "World")
    String name;

    @Inject
    BenchmarkSupplier supplier;

    @RestClient
    ResultProxy resultProxy;

    @Override
    public void run()
    {
        System.out.println("Hello " + name + "!");

        final Infrastructure infrastructure = new Infrastructure();
        final Handler handler = new Handler(infrastructure);
        final Function<Infrastructure, ThroughputResult> function = supplier.get();
        handler.runIteration(new CallableWrapper(function, infrastructure));
    }
}
