package org.openjdk.jmh.generators.core;

import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.logging.Log;
import org.mendrugo.fibula.extension.deployment.JandexGeneratorSource;
import org.mendrugo.fibula.extension.deployment.JandexMethodInfo;
import org.mendrugo.fibula.extension.deployment.Reflection;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.Optional;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A benchmark generator purely used to get a list of all generated benchmark FQNs.
 * The logic in buildAnnotatedSet and makeBenchmarkInfo is non-trivial or too much to just copy/paste, so privately access those.
 * Added in same JMH package in order to access package private classes like BenchmarkInfo.
 */
public final class JmhBenchmarkGenerator extends BenchmarkGenerator
{
    private final Reflection reflection = new Reflection(this, BenchmarkGenerator.class);
    private final Set<BenchmarkInfo> benchmarkInfos = new HashSet<>();

    /**
     * Returns a collection with the fully qualified names of the generated benchmark classes.
     */
    public Collection<String> generate(JandexGeneratorSource source)
    {
        Multimap<ClassInfo, MethodInfo> clazzes = buildAnnotatedSet_(source);

        // Generate code for all found Classes and Methods
        for (ClassInfo clazz : clazzes.keys())
        {
            // todo move isSupportedBenchmarkInfo here so that filtering can be done before creating benchmark info
            if (isSupportedBenchmark(clazz))
            {
                // todo implement validation
                // validateBenchmark(clazz, clazzes.get(clazz));
                Collection<BenchmarkInfo> infos = makeBenchmarkInfo_(clazz, clazzes.get(clazz));
                // generateClass(clazz, info);
                benchmarkInfos.addAll(infos);
            }
        }

        return benchmarkInfos.stream()
            .map(info -> info.generatedClassQName)
            .toList();
    }

    public void complete(BuildSystemTargetBuildItem buildSystemTarget)
    {
        Set<BenchmarkListEntry> entries = new HashSet<>();

        // Generate new benchmark entries
        for (BenchmarkInfo info : benchmarkInfos)
        {
            MethodGroup group = info.methodGroup;
            for (Mode m : group.getModes()) {
                BenchmarkListEntry br = new BenchmarkListEntry(
                    info.userClassQName
                    , info.generatedClassQName
                    , group.getName()
                    , m
                    , group.getTotalThreadCount()
                    , group.getGroupThreads()
                    , group.getGroupLabels()
                    , group.getWarmupIterations()
                    , group.getWarmupTime()
                    , group.getWarmupBatchSize()
                    , group.getMeasurementIterations()
                    , group.getMeasurementTime()
                    , group.getMeasurementBatchSize()
                    , group.getForks()
                    , group.getWarmupForks()
                    , group.getJvm()
                    , group.getJvmArgs()
                    , group.getJvmArgsPrepend()
                    , group.getJvmArgsAppend()
                    , group.getParams()
                    , getOutputTimeUnit(group)
                    , group.getOperationsPerInvocation()
                    , group.getTimeout()
                );

                entries.add(br);
            }
        }

        final File resourceDir = buildSystemTarget.getOutputDirectory().resolve(Path.of("classes")).toFile();
        final FileSystemDestination destination = new FileSystemDestination(resourceDir, null);
        try (OutputStream stream = destination.newResource(BenchmarkList.BENCHMARK_LIST.substring(1)))
        {
            BenchmarkList.writeBenchmarkList(stream, entries);
        }
        catch (IOException ex)
        {
            destination.printError("Error writing benchmark list", ex);
        }
    }

    private static Optional<TimeUnit> getOutputTimeUnit(MethodGroup methodGroup)
    {
        for (MethodInfo methodInfo : methodGroup.methods())
        {
            final Optional<TimeUnit> timeUnit = timeUnit((JandexMethodInfo) methodInfo);
            if (timeUnit.hasValue())
            {
                return timeUnit;
            }
        }

        return Optional.none();
    }

    private static Optional<TimeUnit> timeUnit(JandexMethodInfo method)
    {
        OutputTimeUnit timeUnit;
        if ((timeUnit = method.getAnnotation(OutputTimeUnit.class)) != null
            || (timeUnit = method.getDeclaringClass().getAnnotation(OutputTimeUnit.class)) != null
        )
        {
            return Optional.of(timeUnit.value());
        }

        return Optional.none();
    }

    // todo temporary until all samples are covered
    private static boolean isSupportedBenchmark(ClassInfo info)
    {
        final String fqn = info.getQualifiedName();
        final boolean supported = isSupported(fqn);
        Log.debugf("Benchmark class %s is%s supported", fqn, supported ? "" : " not");
        return supported;
    }

    private static boolean isSupported(String fqn)
    {
        if (fqn.startsWith("org.mendrugo.fibula"))
        {
            return true;
        }

        if (fqn.startsWith("org.openjdk.jmh.it"))
        {
            return fqn.endsWith("interorder.BenchmarkStateOrderTest")
                || fqn.startsWith("profilers.LinuxPerfProfiler")
                || fqn.startsWith("profilers.LinuxPerfNormProfilerTest");
        }

        if (fqn.startsWith("org.openjdk.jmh.samples"))
        {
            return fqn.contains("JMHSample_01")
            || fqn.contains("JMHSample_03")
            || fqn.contains("JMHSample_04")
            || fqn.contains("JMHSample_09");
        }

        return true;
    }


    private Multimap<ClassInfo, MethodInfo> buildAnnotatedSet_(GeneratorSource source)
    {
        return reflection.invoke("buildAnnotatedSet", source, GeneratorSource.class);
    }

    private Collection<BenchmarkInfo> makeBenchmarkInfo_(ClassInfo clazz, Collection<MethodInfo> methods)
    {
        return reflection.invoke("makeBenchmarkInfo", clazz, ClassInfo.class, methods, Collection.class);
    }
}
