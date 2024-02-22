package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.VmInfo;

@Path("/vm")
public class VmResource
{
    @Inject
    VmService vmService;

    @POST
    public String set(VmInfo info)
    {
        Log.debugf("Received VM info: %s", info);
        vmService.setInfo(info);
        return "Ok";
    }
}
