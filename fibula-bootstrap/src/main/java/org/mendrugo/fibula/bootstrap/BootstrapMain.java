package org.mendrugo.fibula.bootstrap;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain
{
    public static void main(String... args)
    {
        System.out.println("Running fibula.bootstrap.BootstrapMain...");
        Quarkus.run(args);
    }
}
