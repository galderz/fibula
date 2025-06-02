package org.mendrugo.fibula;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptions;

import java.io.IOException;

public class MutiVmMain extends Main
{
    public static void main(String[] initialArgs) throws IOException
    {
        final MutiVmMain main = new MutiVmMain();

        final Pgo pgo = Pgo.instance();
        final String[] args = pgo.preProcessArgs(initialArgs);

        final CommandLineOptions cmdOptions = main.createCommandLineOptions(args);

        final Runner runner = pgo.isEnabled()
            ? new MultiVmPgoRunner(cmdOptions)
            : new MultiVmRunner(cmdOptions);

        main.run(cmdOptions, runner);
    }
}
