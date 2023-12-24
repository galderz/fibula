package org.mendrugo.fibula.bootstrap;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain
{
    public static void main(String... args)
    {
        System.out.println("Running bootstrap main...");
        // todo I need bootstrap lifecycle?
        // e.g. I can fire the build and the first invocation/fork
        //      then the result service can fire more if neeeded
        //      and eventually exit
        Quarkus.run(args);
    }
}
