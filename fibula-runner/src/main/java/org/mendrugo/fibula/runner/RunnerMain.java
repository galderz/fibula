package org.mendrugo.fibula.runner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.openjdk.jmh.runner.DualForkedMain;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    @Override
    public int run(String... args) throws Exception
    {
        final Cli cli = Cli.read(args);
        final String host = cli.text("host");
        final String port = cli.text("port");
        DualForkedMain.main(new String[]{host, port});
        return 0;
    }
}
