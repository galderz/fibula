package org.mendrugo.fibula;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.profile.ProfilerOptionFormatter;
import org.openjdk.jmh.profile.ProfilerUtils;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NativeAsyncProfiler implements ExternalProfiler
{
    private final boolean verbose;
    private final String profilerConfig;
    private final List<OutputType> output;
    private final String outputFilePrefix;
    private final File outDir;
    private File trialOutDir;
    private final int traces;
    private final int flat;

    private final String ldPreload;

    public NativeAsyncProfiler(String initLine) throws ProfilerException
    {
        OptionParser parser = new OptionParser();

        parser.formatHelpWith(new ProfilerOptionFormatter("async"));

        OptionSpec<OutputType> optOutput = parser.accepts("output",
                "Output format(s). Supported: " + EnumSet.allOf(OutputType.class) + ".")
            .withRequiredArg().ofType(OutputType.class).withValuesSeparatedBy(",").describedAs("format+").defaultsTo(OutputType.flamegraph);

        OptionSpec<String> optLibPath = parser.accepts("libPath",
                "Location of asyncProfiler library. If not specified, System.loadLibrary will be used " +
                    "and the library must be made available to the forked JVM in an entry of -Djava.library.path, " +
                    "LD_LIBRARY_PATH (Linux), or DYLD_LIBRARY_PATH (Mac OS).")
            .withRequiredArg().ofType(String.class).describedAs("path");

        OptionSpec<String> optEvent = parser.accepts("event",
                "Event to sample: cpu, alloc, lock, wall, itimer; com.foo.Bar.methodName; any event from `perf list` e.g. cache-misses")
            .withRequiredArg().ofType(String.class).describedAs("event").defaultsTo("cpu");

        String secondaryEventOk = "May be captured as a secondary event under output=jfr.";
        OptionSpec<String> optAlloc = parser.accepts("alloc",
                "Enable allocation profiling. Optional argument (e.g. =512k) reduces sampling from the default of one-sample-per-TLAB. " + secondaryEventOk)
            .withOptionalArg().ofType(String.class).describedAs("sample bytes");

        OptionSpec<String> optLock = parser.accepts("lock",
                "Enable lock profiling. Optional argument (e.g. =1ms) limits capture based on lock duration. " + secondaryEventOk)
            .withOptionalArg().ofType(String.class).describedAs("duration");

        OptionSpec<String> optDir = parser.accepts("dir",
                "Output directory.")
            .withRequiredArg().ofType(String.class).describedAs("dir");

        OptionSpec<Long> optInterval = parser.accepts("interval",
                "Profiling interval.")
            .withRequiredArg().ofType(Long.class).describedAs("ns");

        OptionSpec<Integer> optJstackDepth = parser.accepts("jstackdepth",
                "Maximum Java stack depth.")
            .withRequiredArg().ofType(Integer.class).describedAs("frames");

        OptionSpec<Long> optFrameBuf = parser.accepts("framebuf",
                "Size of profiler framebuffer.")
            .withRequiredArg().ofType(Long.class).describedAs("bytes");

        OptionSpec<Boolean> optFilter = parser.accepts("filter",
                "Enable thread filtering during collection. Useful for wall clock profiling, " +
                    "but only if the workload registers the relevant threads programatically " +
                    "via `AsyncProfiler.JavaApi.getInstance().filterThread(thread, enabled)`.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false).describedAs("boolean");

        OptionSpec<Boolean> optThreads = parser.accepts("threads",
                "Profile threads separately.")
            .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optSimple = parser.accepts("simple",
                "Simple class names instead of FQN.")
            .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optSig = parser.accepts("sig",
                "Print method signatures.")
            .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optAnn = parser.accepts("ann",
                "Annotate Java method names.")
            .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<String> optInclude = parser.accepts("include",
                "Output only stack traces containing the specified pattern.")
            .withRequiredArg().withValuesSeparatedBy(",").ofType(String.class).describedAs("regexp+");

        OptionSpec<String> optExclude = parser.accepts("exclude",
                "Exclude stack traces with the specified pattern.")
            .withRequiredArg().withValuesSeparatedBy(",").ofType(String.class).describedAs("regexp+");

        OptionSpec<String> optRawCommand = parser.accepts("rawCommand",
                "Command to pass directly to async-profiler. Use to access new features of JMH " +
                    "profiler that are not yet supported in this option parser.")
            .withRequiredArg().ofType(String.class).describedAs("command");

        OptionSpec<String> optTitle = parser.accepts("title",
                "SVG title.")
            .withRequiredArg().ofType(String.class).describedAs("string");

        OptionSpec<Long> optWidth = parser.accepts("width",
                "SVG width.")
            .withRequiredArg().ofType(Long.class).describedAs("pixels");

        OptionSpec<Long> optMinWidth = parser.accepts("minwidth", "Skip frames smaller than px")
            .withRequiredArg().ofType(Long.class).describedAs("pixels");

        OptionSpec<Boolean> optAllKernel = parser.accepts("allkernel",
                "Only include kernel-mode events.")
            .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<Boolean> optAllUser = parser.accepts("alluser",
                "Only include user-mode events.")
            .withRequiredArg().ofType(Boolean.class).describedAs("bool");

        OptionSpec<CStackMode> optCStack = parser.accepts("cstack",
                "How to traverse C stack: Supported: " + EnumSet.allOf(AsyncProfiler.CStackMode.class) + ".")
            .withRequiredArg().ofType(CStackMode.class).describedAs("mode").defaultsTo(CStackMode.dwarf);

        OptionSpec<Boolean> optVerbose = parser.accepts("verbose",
                "Output the sequence of commands.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false).describedAs("bool");

        OptionSpec<Integer> optTraces = parser.accepts("traces",
                "Number of top traces to include in the default output.")
            .withRequiredArg().ofType(Integer.class).defaultsTo(200).describedAs("int");

        OptionSpec<Integer> optFlat = parser.accepts("flat",
                "Number of top flat profiles to include in the default output.")
            .withRequiredArg().ofType(Integer.class).defaultsTo(200).describedAs("int");

        OptionSet set = ProfilerUtils.parseInitLine(initLine, parser);

        try
        {
            ProfilerOptionsBuilder builder = new ProfilerOptionsBuilder(set);

            if (!set.has(optDir))
            {
                outDir = new File(System.getProperty("user.dir"));
            }
            else
            {
                outDir = new File(set.valueOf(optDir));
            }

            builder.appendIfExists(optInterval);
            builder.appendIfExists(optJstackDepth);
            builder.appendIfTrue(optThreads);
            builder.appendIfTrue(optSimple);
            builder.appendIfTrue(optSig);
            builder.appendIfTrue(optAnn);
            builder.appendIfExists(optFrameBuf);
            if (optFilter.value(set))
            {
                builder.appendRaw("filter");
            }
            builder.appendMulti(optInclude);
            builder.appendMulti(optExclude);

            builder.appendIfExists(optTitle);
            builder.appendIfExists(optWidth);
            builder.appendIfExists(optMinWidth);

            builder.appendIfTrue(optAllKernel);
            builder.appendIfTrue(optAllUser);
            builder.appendIfExists(optCStack);

            if (set.has(optRawCommand))
            {
                builder.appendRaw(optRawCommand.value(set));
            }

            traces = optTraces.value(set);
            flat = optFlat.value(set);

            String envLdPreload = System.getenv("LD_PRELOAD");
            if (envLdPreload != null)
            {
                ldPreload = envLdPreload;
            }
            else if (set.has(optLibPath))
            {
                ldPreload = optLibPath.value(set);
            }
            else
            {
                throw new ProfilerException("Async Profiler library path must be provided via LD_PRELOAD or libPath");
            }

            verbose = optVerbose.value(set);

            output = optOutput.values(set);

            // Secondary events are those that may be collected simultaneously with a primary event in a JFR profile.
            // To be used as such, we require they are specifed with the lock and alloc option, rather than event=lock,
            // event=alloc.
            Set<String> secondaryEvents = new HashSet<>();

            if (set.has(optAlloc))
            {
                secondaryEvents.add("alloc");
                builder.append(optAlloc);
            }

            if (set.has(optLock))
            {
                secondaryEvents.add("lock");
                builder.append(optLock);
            }

            if (set.has(optEvent))
            {
                String evName = set.valueOf(optEvent);
                if (evName.contains(","))
                {
                    throw new ProfilerException("Event name should not contain commas: " + evName);
                }
                outputFilePrefix = evName;
                builder.append(optEvent);
            } else {
                if (secondaryEvents.isEmpty())
                {
                    // Default to the cpu event if no events at all are selected.
                    builder.appendRaw("event=cpu");
                    outputFilePrefix = "cpu";
                } else if (secondaryEvents.size() == 1)
                {
                    // No primary event, one secondary -- promote it to the primary event. This means any output
                    // format is allowed and the event name will be included in the output file name.
                    outputFilePrefix = secondaryEvents.iterator().next();
                    secondaryEvents.clear();
                } else
                {
                    outputFilePrefix = "profile";
                }
            }

            if (!secondaryEvents.isEmpty())
            {
                if (output.size() > 1 || output.get(0) != OutputType.jfr)
                {
                    throw new ProfilerException("Secondary event capture is only supported with output=" + AsyncProfiler.OutputType.jfr.name());
                }
            }

            profilerConfig = builder.profilerOptions();
        }
        catch (OptionException e)
        {
            throw new ProfilerException(e.getMessage());
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params)
    {
        final List<String> jvmInvokeOpts = new ArrayList<>();
        jvmInvokeOpts.add("env");
        jvmInvokeOpts.add("LD_PRELOAD=" + ldPreload);
        String asprofCommand =
            output.contains(OutputType.jfr)
            ? "start," + profilerConfig + ",file=" + jfrOutputFile().getAbsolutePath()
            : "start," + profilerConfig + ",file=" + flameOutputFile().getAbsolutePath();
        jvmInvokeOpts.add("ASPROF_COMMAND=" + asprofCommand);
        return jvmInvokeOpts;
    }

    private File jfrOutputFile() {
        return outputFile("jfr-%s.jfr");
    }

    private File flameOutputFile() {
        return outputFile("flame-%s.html");
    }

    private File outputFile(String fileNameFormat) {
        return new File(trialOutDir, String.format(fileNameFormat, outputFilePrefix));
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params)
    {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams)
    {
        // Nothing to do
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr)
    {
        List<Result<?>> results = new ArrayList<>();
        for (OutputType outputType : output)
        {
            switch (outputType)
            {
                case flamegraph:
                    // Flame graph is already dumped into file by async-profiler.
                    results.add(new FileResult("async-flamegraph", Collections.singletonList(flameOutputFile())));
                    break;
                case jfr:
                    // JFR is already dumped into file by async-profiler.
                    results.add(new FileResult("async-jfr", Collections.singletonList(jfrOutputFile())));
                    break;
            }
        }

        return results;
    }

    @Override
    public boolean allowPrintOut()
    {
        return true;
    }

    @Override
    public boolean allowPrintErr()
    {
        return true;
    }

    @Override
    public String getDescription()
    {
        return "async-profiler native profiler provider.";
    }

    public enum OutputType
    {
        flamegraph,
        jfr
    }

    public enum Direction
    {
        forward,
        reverse,
        both,
    }

    public enum CStackMode
    {
        dwarf
    }

    private static class ProfilerOptionsBuilder
    {
        private final OptionSet optionSet;
        private final StringBuilder profilerOptions;

        ProfilerOptionsBuilder(OptionSet optionSet)
        {
            this.optionSet = optionSet;
            this.profilerOptions = new StringBuilder();
        }

        <T> void appendIfExists(OptionSpec<T> option)
        {
            if (optionSet.has(option))
            {
                append(option);
            }
        }

        <T> void append(OptionSpec<T> option)
        {
            assert (option.options().size() == 1);
            String optionName = option.options().iterator().next();
            separate();
            profilerOptions.append(optionName);
            T arg = optionSet.valueOf(option);
            if (arg != null)
            {
                profilerOptions.append('=').append(arg);
            }
        }

        void appendRaw(String command)
        {
            separate();
            profilerOptions.append(command);
        }

        private void separate()
        {
            if (profilerOptions.length() > 0)
            {
                profilerOptions.append(',');
            }
        }

        void appendIfTrue(OptionSpec<Boolean> option)
        {
            if (optionSet.has(option) && optionSet.valueOf(option))
            {
                append(option);
            }
        }

        <T> void appendMulti(OptionSpec<T> option)
        {
            if (optionSet.has(option))
            {
                assert (option.options().size() == 1);
                String optionName = option.options().iterator().next();
                for (T value : optionSet.valuesOf(option))
                {
                    separate();
                    profilerOptions.append(optionName).append('=').append(value.toString());
                }
            }
        }

        public String profilerOptions()
        {
            return profilerOptions.toString();
        }
    }

    public final static class FileResult extends Result<FileResult>
    {
        private final List<File> files;

        FileResult(String label, List<File> files)
        {
            super(ResultRole.SECONDARY, label, of(Double.NaN), "---", AggregationPolicy.AVG);
            this.files = files;
        }

        @Override
        protected Aggregator<FileResult> getThreadAggregator()
        {
            return new FileResult.FileAggregator();
        }

        @Override
        protected Aggregator<FileResult> getIterationAggregator()
        {
            return new FileResult.FileAggregator();
        }

        public Collection<? extends File> getFiles()
        {
            return files;
        }

        @Override
        public String toString()
        {
            return "Files: " + files;
        }

        @Override
        public String extendedInfo()
        {
            StringBuilder builder = new StringBuilder("Async profiler results:")
                .append(System.lineSeparator());
            for (File file : files)
            {
                builder.append("  ")
                    .append(file.getPath())
                    .append(System.lineSeparator());
            }
            return builder.toString();
        }

        private static class FileAggregator implements Aggregator<FileResult>
        {
            @Override
            public FileResult aggregate(Collection<FileResult> results)
            {
                return new FileResult(results.iterator().next().getLabel(),
                    results.stream()
                        .flatMap(r -> r.files.stream())
                        .distinct()
                        .collect(Collectors.toList())
                );
            }
        }
    }
}
