package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.VmInfo;

@ApplicationScoped
public class VmInfoService
{
    private VmInfo info;

    void set(VmInfo info)
    {
        this.info = info;
    }

    String jdkVersion()
    {
        return info.jdkVersion();
    }

    String vmName()
    {
        return info.vmName();
    }

    String vmVersion()
    {
        return info.vmVersion();
    }
}