package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.NativeBenchmarkTaskResult;

@Path("/results")
public class ResultResource
{
    @POST
    public String add(NativeBenchmarkTaskResult result)
    {
        Log.infof("Received result: %s", result);
        // new ResultFormat().write(result);
        Log.infof("Now exit the application");
        Quarkus.asyncExit();
        return "Ok";
    }
}