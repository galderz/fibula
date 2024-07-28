package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.IterationError;
import org.mendrugo.fibula.results.Serializables;

@Path("/iteration")
public class IterationResource
{
    @Inject
    ResultService resultService;

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
