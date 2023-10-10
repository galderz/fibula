package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    private static final Logger LOGGER = Logger.getLogger(RunnerMain.class);

//    @RestClient
//    ResultProxy resultProxy;

    @Inject
    @All
    List<BenchmarkSupplier> suppliers;

    @Override
    public int run(String... args)
    {
        System.out.println("Running fibula.runner.RunnerMain...");
        suppliers.forEach(BenchmarkSupplier::run);
        return 0;
    }
}
