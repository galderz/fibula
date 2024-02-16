package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.VmInfo;

@Path("/vm/info")
public class VmInfoResource
{
    @Inject
    VmInfoService infoService;

    @POST
    public String set(VmInfo info)
    {
        Log.debugf("Received VM info: %s", info);
        infoService.set(info);
        return "Ok";
    }
}
