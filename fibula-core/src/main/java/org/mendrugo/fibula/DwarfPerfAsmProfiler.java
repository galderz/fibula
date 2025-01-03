package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.profile.ProfilerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class DwarfPerfAsmProfiler extends LinuxPerfAsmProfiler
{
    public DwarfPerfAsmProfiler(String initLine) throws ProfilerException
    {
        // Skip asm because functionality is not yet implemented.
        // Instead, safe the perf bin file and analyze it with perf annotate.
        super(initLine + ";skipAsm=true;savePerfBin=true");
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
