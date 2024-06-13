package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.profile.ProfilerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DwarfPerfAsmProfiler extends LinuxPerfAsmProfiler
{
    public DwarfPerfAsmProfiler(String initLine) throws ProfilerException
    {
        super(initLine);
    }

    // Empty constructor for service loader
    public DwarfPerfAsmProfiler() throws ProfilerException
    {
        super("");
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params)
    {
        final Collection<String> perfAsmOptions = super.addJVMInvokeOptions(params);
        final List<String> result = new ArrayList<>(perfAsmOptions);
        result.add("--call-graph");
        result.add("dwarf");
        return result;
    }

    @Override
    protected void parseEvents()
    {
        // Skip parsing events because post-processing will be done with CLI tools.
    }
}
