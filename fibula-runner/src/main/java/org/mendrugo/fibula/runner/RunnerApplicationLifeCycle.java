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
public class RunnerApplicationLifeCycle
{
    private static final Logger LOGGER = Logger.getLogger(RunnerApplicationLifeCycle.class);

    @RestClient
    ResultProxy resultProxy;

    void onStart(@Observes StartupEvent ev)
    {
        LOGGER.info("-> onStart");
        resultProxy.send(ThroughputResult.of("sample-label", 1, 2000, 1000));
    }

    void onStop(@Observes ShutdownEvent ev)
    {
        LOGGER.info("-> onStop");
    }
}
