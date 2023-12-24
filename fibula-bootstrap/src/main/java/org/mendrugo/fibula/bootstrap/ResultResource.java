package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.NativeIterationResult;

@Path("/results")
public class ResultResource
{
    @Inject
    ResultService resultService;

    @POST
    public String add(NativeIterationResult result)
    {
        Log.infof("Received result: %s", result);
        resultService.addIteration(result);
        // new ResultFormat().write(result);
        // endRun(result);
        // Log.infof("Now exit the application");
        // Quarkus.asyncExit();
        return "Ok";
    }
}
