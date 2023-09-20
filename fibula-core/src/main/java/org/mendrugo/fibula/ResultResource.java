package org.mendrugo.fibula;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.ThroughputResult;

@Path("/api/results")
public class ResultResource
{
    @POST
    public String add(ThroughputResult result)
    {
        System.out.println("Received: " + result);
        return "OK";
    }
}
