package org.mendrugo.fibula;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.profile.ProfilerOptionFormatter;
import org.openjdk.jmh.profile.ProfilerUtils;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.TextResult;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.TempFile;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class PerfDwarfProfiler implements ExternalProfiler
{
    private final TempFile perfBinData;
    private final OptionSet set;
    private final String savePerfBinTo;
    private final String savePerfBinFile;
    private final String sampleFrequency;
    private final List<String> requestedEventNames;

    public PerfDwarfProfiler(String initLine) throws ProfilerException
    {
        try
        {
            perfBinData = FileUtils.weakTempFile("perfbin");
        }
        catch (IOException e)
        {
            throw new ProfilerException(e);
        }


        final OptionParser parser = new OptionParser();
        parser.formatHelpWith(new ProfilerOptionFormatter("org.mendrugo.fibula.PerfDwarfProfiler"));

        final OptionSpec<String> optPerfBinTo = parser.accepts(
            "savePerfBinTo"
                , "Override the binary perf data location. This will use the unique file name per test. Use this for debugging."
            )
            .withRequiredArg()
            .ofType(String.class)
            .describedAs("dir")
            .defaultsTo(".");

        final OptionSpec<String> optPerfBinToFile = parser.accepts(
            "savePerfBinToFile"
                , "Override the perf binary data filename. Use this for debugging."
            )
            .withRequiredArg()
            .ofType(String.class)
            .describedAs("file");

        final OptionSpec<String> optFrequency = parser.accepts(
                "frequency"
                , "Sampling frequency, synonymous to perf record --freq #; " + "use \"max\" for highest sampling rate possible on the system."
            )
            .withRequiredArg()
            .ofType(String.class)
            .describedAs("freq")
            .defaultsTo("1000");

        final OptionSpec<String> optEvents = parser.accepts(
            "events"
                , "Events to gather."
            )
            .withRequiredArg()
            .ofType(String.class)
            .withValuesSeparatedBy(",")
            .describedAs("event")
            .defaultsTo("cycles");

        set = ProfilerUtils.parseInitLine(initLine, parser);

        try
        {
            savePerfBinTo = set.valueOf(optPerfBinTo);
            savePerfBinFile = set.valueOf(optPerfBinToFile);
            sampleFrequency = set.valueOf(optFrequency);
            requestedEventNames = set.valuesOf(optEvents);
        }
        catch (OptionException | IllegalArgumentException e)
        {
            throw new ProfilerException(e);
        }
    }

    // Empty constructor for service loader
    public PerfDwarfProfiler() throws ProfilerException
    {
        this("");
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params)
    {
        return List.of(
            "perf"
            , "record"
            , "--freq"
            , String.valueOf(sampleFrequency)
            , "--event", Utils.join(requestedEventNames, ",")
            , "--output", perfBinData.getAbsolutePath()
            , "--call-graph"
            , "dwarf"
        );
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params)
    {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams)
    {
        // Do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr)
    {
        final StringWriter outText = new StringWriter();
        final PrintWriter out = new PrintWriter(outText);

        String target = (savePerfBinFile == null)
            ? savePerfBinTo + "/" + br.getParams().id() + ".perfbin"
            : savePerfBinFile;
        try
        {
            FileUtils.copy(perfBinData.getAbsolutePath(), target);
            out.println("Perf binary output saved to " + target);
        }
        catch (IOException e)
        {
            out.println("Unable to save perf binary output to " + target);
        }

        perfBinData.delete();

        return Collections.singleton(new TextResult(outText.toString(), "dwarf"));
    }

    @Override
    public boolean allowPrintOut()
    {
        return false;
    }

    @Override
    public boolean allowPrintErr()
    {
        return false;
    }

    @Override
    public String getDescription()
    {
        return "DWARF perf Profiler";
    }

}
