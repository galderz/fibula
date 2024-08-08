package org.mendrugo.fibula.runner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.RunnerArguments;
import org.openjdk.jmh.runner.DualForkedMain;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    @RestClient
    IterationClient iterationClient;

    @Inject
    OutputFormatDelegate outputFormat;

    @Override
    public int run(String... args) throws Exception
    {
        final Cli cli = Cli.read(args);
        final String host = cli.text(RunnerArguments.HOST);
        final String port = cli.text(RunnerArguments.PORT);
        DualForkedMain.main(new String[]{host, port});
        return 0;
    }
}
