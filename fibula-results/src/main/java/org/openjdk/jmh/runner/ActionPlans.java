package org.openjdk.jmh.runner;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;
import org.openjdk.jmh.util.Version;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Workaround for package private org.openjdk.jmh.runner.ActionType definition.
 */
public final class ActionPlans
{
    /**
     * Workaround package private org.openjdk.jmh.runner.Action class
     */
    public static BenchmarkParams getParams(ActionPlan actionPlan)
    {
        return actionPlan.getMeasurementActions().get(0).getParams();
    }

    /**
     * Copied because it's private.
     */
    public static List<ActionPlan> getActionPlans(Set<BenchmarkListEntry> benchmarks, BenchmarkList list, Options options, OutputFormat out)
    {
        ActionPlan base = new ActionPlan(ActionType.FORKED);

        LinkedHashSet<BenchmarkListEntry> warmupBenches = new LinkedHashSet<>();

        List<String> warmupMicrosRegexp = options.getWarmupIncludes();
        if (warmupMicrosRegexp != null && !warmupMicrosRegexp.isEmpty())
        {
            warmupBenches.addAll(list.find(out, warmupMicrosRegexp, Collections.<String>emptyList()));
        }
        if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isBulk())
        {
            warmupBenches.addAll(benchmarks);
        }

        for (BenchmarkListEntry wr : warmupBenches)
        {
            base.add(newAction(wr, ActionMode.WARMUP, options, out));
        }

        ActionPlan embeddedPlan = new ActionPlan(ActionType.EMBEDDED);
        embeddedPlan.mixIn(base);

        boolean addEmbedded = false;

        List<ActionPlan> result = new ArrayList<>();
        for (BenchmarkListEntry br : benchmarks)
        {
            BenchmarkParams params = newBenchmarkParams(br, ActionMode.UNDEF, options, out);

            if (params.getForks() <= 0)
            {
                if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isIndi())
                {
                    embeddedPlan.add(newAction(br, ActionMode.WARMUP_MEASUREMENT, options, out));
                }
                else
                {
                    embeddedPlan.add(newAction(br, ActionMode.MEASUREMENT, options, out));
                }
                addEmbedded = true;
            }

            if (params.getForks() > 0)
            {
                ActionPlan r = new ActionPlan(ActionType.FORKED);
                r.mixIn(base);
                if (options.getWarmupMode().orElse(Defaults.WARMUP_MODE).isIndi())
                {
                    r.add(newAction(br, ActionMode.WARMUP_MEASUREMENT, options, out));
                }
                else
                {
                    r.add(newAction(br, ActionMode.MEASUREMENT, options, out));
                }
                result.add(r);
            }
        }

        if (addEmbedded)
        {
            result.add(embeddedPlan);
        }

        return result;
    }

    /**
     * Copied because it's private.
     */
    private static Action newAction(BenchmarkListEntry br, ActionMode mode, Options options, OutputFormat out)
    {
        return new Action(newBenchmarkParams(br, mode, options, out), mode);
    }

    /**
     * Copied because it's private.
     */
    private static BenchmarkParams newBenchmarkParams(BenchmarkListEntry benchmark, ActionMode mode, Options options, OutputFormat out)
    {
        int cpuCount = 0;
        int[] threadGroups = options.getThreadGroups().orElse(benchmark.getThreadGroups());

        int threads = options.getThreads().orElse(
            benchmark.getThreads().orElse(
                Defaults.THREADS));

        if (threads == Threads.MAX)
        {
            if (cpuCount == 0)
            {
                out.print("# Detecting actual CPU count: ");
                cpuCount = Utils.figureOutHotCPUs();
                out.println(cpuCount + " detected");
            }
            threads = cpuCount;
        }

        threads = Utils.roundUp(threads, Utils.sum(threadGroups));

        boolean synchIterations = (benchmark.getMode() != Mode.SingleShotTime) &&
            options.shouldSyncIterations().orElse(Defaults.SYNC_ITERATIONS);

        IterationParams measurement = mode.doMeasurement() ?
            new IterationParams(
                IterationType.MEASUREMENT,
                options.getMeasurementIterations().orElse(
                    benchmark.getMeasurementIterations().orElse(
                        (benchmark.getMode() == Mode.SingleShotTime) ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT : Defaults.MEASUREMENT_ITERATIONS
                    )),
                options.getMeasurementTime().orElse(
                    benchmark.getMeasurementTime().orElse(
                        (benchmark.getMode() == Mode.SingleShotTime) ? TimeValue.NONE : Defaults.MEASUREMENT_TIME
                    )),
                options.getMeasurementBatchSize().orElse(
                    benchmark.getMeasurementBatchSize().orElse(
                        Defaults.MEASUREMENT_BATCHSIZE
                    )
                )
            ) :
            new IterationParams(IterationType.MEASUREMENT, 0, TimeValue.NONE, 1);

        IterationParams warmup = mode.doWarmup() ?
            new IterationParams(
                IterationType.WARMUP,
                options.getWarmupIterations().orElse(
                    benchmark.getWarmupIterations().orElse(
                        (benchmark.getMode() == Mode.SingleShotTime) ? Defaults.WARMUP_ITERATIONS_SINGLESHOT : Defaults.WARMUP_ITERATIONS
                    )),
                options.getWarmupTime().orElse(
                    benchmark.getWarmupTime().orElse(
                        (benchmark.getMode() == Mode.SingleShotTime) ? TimeValue.NONE : Defaults.WARMUP_TIME
                    )),
                options.getWarmupBatchSize().orElse(
                    benchmark.getWarmupBatchSize().orElse(
                        Defaults.WARMUP_BATCHSIZE
                    )
                )
            ) :
            new IterationParams(IterationType.WARMUP, 0, TimeValue.NONE, 1);

        int forks = options.getForkCount().orElse(
            benchmark.getForks().orElse(
                Defaults.MEASUREMENT_FORKS));

        int warmupForks = options.getWarmupForkCount().orElse(
            benchmark.getWarmupForks().orElse(
                Defaults.WARMUP_FORKS));

        TimeUnit timeUnit = options.getTimeUnit().orElse(
            benchmark.getTimeUnit().orElse(
                Defaults.OUTPUT_TIMEUNIT));

        int opsPerInvocation = options.getOperationsPerInvocation().orElse(
            benchmark.getOperationsPerInvocation().orElse(
                Defaults.OPS_PER_INVOCATION));

        String jvm = options.getJvm().orElse(
            benchmark.getJvm().orElse(Utils.getCurrentJvm()));

        Properties targetProperties;
        if (jvm.equals(Utils.getCurrentJvm()))
        {
            targetProperties = Utils.getRecordedSystemProperties();
        }
        else
        {
            targetProperties = Utils.readPropertiesFromCommand(getPrintPropertiesCommand(jvm, options, out));
        }

        Collection<String> jvmArgs = new ArrayList<>();

        jvmArgs.addAll(options.getJvmArgsPrepend().orElse(
            benchmark.getJvmArgsPrepend().orElse(Collections.<String>emptyList())));

        // We want to be extra lazy when accessing ManagementFactory, because security manager
        // may prevent us doing so.
        jvmArgs.addAll(options.getJvmArgs()
            .orElseGet(() -> benchmark.getJvmArgs()
                .orElseGet(() -> ManagementFactory.getRuntimeMXBean().getInputArguments())
            ));


        jvmArgs.addAll(options.getJvmArgsAppend().orElse(
            benchmark.getJvmArgsAppend().orElse(Collections.<String>emptyList())));

        TimeValue timeout = options.getTimeout().orElse(
            benchmark.getTimeout().orElse(Defaults.TIMEOUT));

        String jdkVersion = targetProperties.getProperty("java.version");
        String vmVersion = targetProperties.getProperty("java.vm.version");
        String vmName = targetProperties.getProperty("java.vm.name");
        return new BenchmarkParams(benchmark.getUsername(), benchmark.generatedTarget(), synchIterations,
            threads, threadGroups, benchmark.getThreadGroupLabels().orElse(Collections.<String>emptyList()),
            forks, warmupForks,
            warmup, measurement, benchmark.getMode(), benchmark.getWorkloadParams(), timeUnit, opsPerInvocation,
            jvm, jvmArgs,
            jdkVersion, vmName, vmVersion, Version.getPlainVersion(),
            timeout);
    }

    private static List<String> getPrintPropertiesCommand(String jvm, Options options, OutputFormat out) {
        List<String> command = new ArrayList<>();

        // use supplied jvm, if given
        command.add(jvm);

        // assemble final process command
        addClasspath(command, options, out);

        command.add(PrintPropertiesMain.class.getName());

        return command;
    }

    private static void addClasspath(List<String> command, Options options, OutputFormat out) {
        command.add("-cp");

        String cpProp = System.getProperty("java.class.path");
        File tmpFile = null;

        String jvmargs = ""
            + options.getJvmArgs().orElse(Collections.<String>emptyList())
            + options.getJvmArgsPrepend().orElse(Collections.<String>emptyList())
            + options.getJvmArgsAppend().orElse(Collections.<String>emptyList());

        // The second (creepy) test is for the cases when external plugins are not supplying
        // the options properly. Looking at you, JMH Gradle plugin. In this case, we explicitly
        // check if the option is provided by the user.

        if (Boolean.getBoolean("jmh.separateClasspathJAR")
            || jvmargs.contains("jmh.separateClasspathJAR=true"))
        {

            // Classpath can be too long and overflow the command line length.
            // Looking at you, Windows.
            //
            // The trick is to generate the JAR file with appropriate Class-Path manifest entry,
            // and link it. The complication is that Class-Path entry paths are specified relative
            // to JAR file loaded, which is probably somewhere in java.io.tmpdir, outside of current
            // directory. Therefore, we have to relativize the paths to all the JAR entries.

            try
            {
                tmpFile = FileUtils.tempFile("classpath.jar");
                Path tmpFileDir = tmpFile.toPath().getParent();

                StringBuilder sb = new StringBuilder();
                for (String cp : cpProp.split(File.pathSeparator))
                {
                    Path cpPath = new File(cp).getAbsoluteFile().toPath();
                    if (!cpPath.getRoot().equals(tmpFileDir.getRoot()))
                    {
                        throw new IOException("Cannot relativize: " + cpPath + " and " + tmpFileDir + " have different roots.");
                    }

                    Path relPath = tmpFileDir.relativize(cpPath);
                    if (!Files.isReadable(tmpFileDir.resolve(relPath)))
                    {
                        throw new IOException("Cannot read through the relativized path: " + relPath);
                    }

                    String rel = relPath.toString();
                    sb.append(rel.replace('\\', '/').replace(" ", "%20"));
                    if (Files.isDirectory(cpPath))
                    {
                        sb.append('/');
                    }
                    sb.append(" ");
                }
                String classPath = sb.toString().trim();

                Manifest manifest = new Manifest();
                Attributes attrs = manifest.getMainAttributes();
                attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
                attrs.putValue("Class-Path", classPath);

                try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tmpFile), manifest))
                {
                    jos.putNextEntry(new ZipEntry("META-INF/"));
                }

                out.verbosePrintln("Using separate classpath JAR: " + tmpFile);
                out.verbosePrintln("  Class-Path: " + classPath);
            }
            catch (IOException ex)
            {
                // Something is wrong in file generation, give up and fall-through to usual thing
                out.verbosePrintln("Caught IOException when building separate classpath JAR: " +
                    ex.getMessage() + ", falling back to default -cp.");
                tmpFile = null;
            }
        }

        if (tmpFile != null)
        {
            if (Utils.isWindows())
            {
                command.add("\"" + tmpFile.getAbsolutePath() + "\"");
            }
            else
            {
                command.add(tmpFile.getAbsolutePath());
            }
        }
        else
        {
            if (Utils.isWindows())
            {
                command.add('"' + cpProp + '"');
            }
            else
            {
                command.add(cpProp);
            }
        }
    }

    private ActionPlans()
    {
    }
}
