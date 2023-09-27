package org.mendrugo.fibula.runner;

import io.quarkus.arc.All;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@QuarkusMain
public class BenchmarkMain implements QuarkusApplication
{
    private static final Logger LOGGER = Logger.getLogger(BenchmarkMain.class);

//    @RestClient
//    ResultProxy resultProxy;

    @Inject
    @All
    List<BenchmarkSupplier> suppliers;

    @Override
    public int run(String... args)
    {
        suppliers.forEach(BenchmarkSupplier::run);
        return 0;
    }
}
