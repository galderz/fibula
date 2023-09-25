package org.mendrugo.fibula.runner;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.mendrugo.fibula.results.ThroughputResult;
import org.mendrugo.fibula.runner.client.ResultProxy;

@ApplicationScoped
public abstract class AbstractBenchmarkRunner
{
    private static final Logger LOGGER = Logger.getLogger(AbstractBenchmarkRunner.class);

    @RestClient
    ResultProxy resultProxy;

    void onStart(@Observes StartupEvent ev)
    {
        LOGGER.info("-> onStart");
//        resultProxy.send(ThroughputResult.of("sample-label", 1, 2000, 1000));
        final Infrastructure infrastructure = new Infrastructure();
        // todo consider using vertx.timer() instead
        final ThroughputResult result = doBenchmark(new Handler(infrastructure), infrastructure);
        resultProxy.send(result);
    }

    public abstract ThroughputResult doBenchmark(Handler handler, Infrastructure infrastructure);

    void onStop(@Observes ShutdownEvent ev)
    {
        LOGGER.info("-> onStop");
    }
}
