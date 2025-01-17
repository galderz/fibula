package org.mendrugo.fibula;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
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
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        parser.formatHelpWith(new ProfilerHelpFormatter());

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

        set = parseInitLine(initLine, parser);

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

        return Collections.singleton(new TextResult(outText.toString(), "asm"));
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

    public static OptionSet parseInitLine(String initLine, OptionParser parser) throws ProfilerException
    {
        parser.accepts("help", "Display help.");

        final OptionSpec<String> nonOptions = parser.nonOptions();

        String[] split = initLine.split(";");
        for (int c = 0; c < split.length; c++)
        {
            if (!split[c].isEmpty())
            {
                split[c] = "-" + split[c];
            }
        }

        OptionSet optionSet;
        try
        {
            optionSet = parser.parse(split);
        }
        catch (OptionException e)
        {
            try
            {
                final StringWriter sw = new StringWriter();
                sw.append(e.getMessage());
                sw.append("\n");
                parser.printHelpOn(sw);
                throw new ProfilerException(sw.toString());
            }
            catch (IOException ioException)
            {
                throw new ProfilerException(ioException);
            }
        }

        if (optionSet.has("help"))
        {
            try
            {
                StringWriter sw = new StringWriter();
                parser.printHelpOn(sw);
                throw new ProfilerException(sw.toString());
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        final String optionArgument = optionSet.valueOf(nonOptions);
        if (optionArgument != null && !optionArgument.isEmpty())
        {
            throw new ProfilerException("Unhandled options: " + optionArgument + " in " + initLine);
        }
        return optionSet;
    }

    private static final class ProfilerHelpFormatter implements HelpFormatter
    {
        @Override
        public String format(Map<String, ? extends OptionDescriptor> options)
        {
            final String header = """
                Usage: -prof org.mendrugo.fibula.PerfDwarfProfiler:opt1=value1,value2;opt2=value3
                
                Options accepted by profiler:
                """;

            final StringBuilder help = new StringBuilder(header);
            for (OptionDescriptor option : options.values())
            {
                help.append(optionHelp(option));
            }

            return help.toString();
        }

        private static String optionHelp(OptionDescriptor descriptor)
        {
            final StringBuilder line = new StringBuilder();

            final StringBuilder output = new StringBuilder();
            output.append("  ");
            for (String str : descriptor.options())
            {
                if (descriptor.representsNonOptions())
                {
                    continue;
                }
                output.append(str);
                if (descriptor.acceptsArguments())
                {
                    output.append("=");
                    if (descriptor.requiresArgument())
                    {
                        output.append("<");
                    }
                    else
                    {
                        output.append("[");
                    }
                    output.append(descriptor.argumentDescription());
                    if (descriptor.requiresArgument())
                    {
                        output.append(">");
                    }
                    else
                    {
                        output.append("]");
                    }
                }
            }

            final int optWidth = 35;
            line.append(String.format("%-" + optWidth + "s", output));
            boolean first = true;
            String desc = descriptor.description();
            final List<?> defaults = descriptor.defaultValues();
            if (defaults != null && !defaults.isEmpty())
            {
                desc += " (default: " + defaults + ")";
            }
            for (String l : Utils.rewrap(desc))
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    line.append(System.lineSeparator());
                    line.append(String.format("%-" + optWidth + "s", ""));
                }
                line.append(l);
            }

            line.append(System.lineSeparator());
            line.append(System.lineSeparator());
            return line.toString();
        }
    }
}
