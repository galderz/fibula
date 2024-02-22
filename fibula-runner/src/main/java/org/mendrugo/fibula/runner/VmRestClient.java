package org.mendrugo.fibula.runner;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.mendrugo.fibula.results.VmInfo;

@Path("/vm")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "bootstrap")
public interface VmRestClient
{
    @POST
    String set(VmInfo info);
}
