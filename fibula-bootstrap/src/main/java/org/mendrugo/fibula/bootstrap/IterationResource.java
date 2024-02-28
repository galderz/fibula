package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.IterationEnd;
import org.mendrugo.fibula.results.IterationStart;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.IterationResult;

public class IterationResource
{
    @Inject
    ResultService resultService;

    @Path("/iteration/start")
    @POST
    public String receive(IterationStart iterationStart)
    {
        Log.debugf("Received: %s", iterationStart);
        final BenchmarkParams benchmarkParams = Serializables.fromBase64(iterationStart.benchmarkParams());
        final IterationParams iterationParams = Serializables.fromBase64(iterationStart.iterationParams());
        resultService.startIteration(benchmarkParams, iterationParams, iterationStart.iteration());
        return "Ok";
    }

    @Path("/iteration/end")
    @POST
    public String receiveIterationEnd(IterationEnd iterationEnd)
    {
        Log.debugf("Received: %s", iterationEnd);
        final IterationResult result = Serializables.fromBase64(iterationEnd.result());
        resultService.endIteration(iterationEnd.iteration(), result);
        return "Ok";
    }
}