package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.IterationEnd;
import org.mendrugo.fibula.results.IterationError;
import org.mendrugo.fibula.results.IterationStart;
import org.mendrugo.fibula.results.IterationTelemetry;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;

@Path("/iteration")
public class IterationResource
{
    @Inject
    ResultService resultService;

//    @Path("/start")
//    @POST
//    public String start(IterationStart iterationStart)
//    {
//        Log.debugf("Received: %s", iterationStart);
//        final BenchmarkParams benchmarkParams = Serializables.fromBase64(iterationStart.benchmarkParams());
//        final IterationParams iterationParams = Serializables.fromBase64(iterationStart.iterationParams());
//        resultService.startIteration(benchmarkParams, iterationParams, iterationStart.iteration());
//        return "Ok";
//    }
//
//    @Path("/end")
//    @POST
//    public String end(IterationEnd iterationEnd)
//    {
//        Log.debugf("Received: %s", iterationEnd);
//        final IterationResult result = Serializables.fromBase64(iterationEnd.result());
//        resultService.endIteration(iterationEnd.iteration(), result);
//        return "Ok";
//    }

    @Path("/error")
    @POST
    public String error(IterationError iterationError)
    {
        Log.debugf("Received: %s", iterationError);
        resultService.errorIteration(iterationError.errorMessage(), iterationError.details());
        return "Ok";
    }

    @Path("/result-metadata")
    @POST
    public String resultMetadata(String resultMetadata)
    {
        Log.debugf("Received: result-metadata %s", resultMetadata);
        resultService.setResultMetadata(Serializables.fromBase64(resultMetadata));
        return "Ok";
    }

    @Path("/result")
    @POST
    public String result(String result)
    {
        Log.debugf("Received: result %s", result);
        resultService.endIteration(Serializables.fromBase64(result));
        return "Ok";
    }
}
