package org.mendrugo.fibula.runner;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.mendrugo.fibula.results.IterationError;

@Path("/iteration")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "bootstrap")
public interface IterationClient
{
    @Path("/error")
    @POST
    String notifyError(IterationError error);

    @Path("/result-metadata")
    @POST
    String notifyResultMetaData(String resultMetaData);

    @Path("/result")
    @POST
    String notifyResult(String result);
}
