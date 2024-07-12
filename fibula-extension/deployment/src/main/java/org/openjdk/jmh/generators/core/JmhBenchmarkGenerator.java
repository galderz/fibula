package org.openjdk.jmh.generators.core;

import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.extension.deployment.JandexGeneratorSource;
import org.mendrugo.fibula.extension.deployment.JandexMethodInfo;
import org.mendrugo.fibula.extension.deployment.Reflection;
import org.mendrugo.fibula.results.Modes;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.results.BenchmarkTaskResult;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.InfraControl;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.Optional;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public final class JmhBenchmarkGenerator extends BenchmarkGenerator
{
    private static final String JMH_STUB_SUFFIX = "_jmhStub";

    private final Reflection reflection = new Reflection(this, BenchmarkGenerator.class);
    private final ClassOutput beanOutput;
    private final ClassOutput classOutput;
    private final Set<BenchmarkInfo> benchmarkInfos = new HashSet<>();

    public JmhBenchmarkGenerator(ClassOutput beanOutput, ClassOutput classOutput)
    {
        this.beanOutput = beanOutput;
        this.classOutput = classOutput;
    }

    public void generate(JandexGeneratorSource source)
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
                for (BenchmarkInfo info : infos)
                {
                    generateClass(clazz, info);
                    // todo move to addAll(infos) when isSupportedBenchmark is removed
                    benchmarkInfos.add(info);
                }
            }
        }
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

    private void generateClass(ClassInfo classInfo, BenchmarkInfo info)
    {
        final CompilerControlPlugin compilerControl = new CompilerControlPlugin();

        // write all methods
        Modes.nonAll().forEach(benchmarkKind ->
        {
            final String prefix = String.format(
                "%s.%s_%s_%s"
                , info.generatedPackageName
                , info.generatedClassName
                , info.methodGroup.getName()
                , benchmarkKind.name()
            );

            final String functionFqn = String.format("%s_BiFunction", prefix);

            try (final ClassCreator function = ClassCreator.builder()
                .classOutput(classOutput)
                .className(functionFqn)
                .interfaces(BiFunction.class)
                .build()
            )
            {
                addMethods(benchmarkKind, info, function);
                
            }

            final String supplierFqn = String.format("%s_Supplier", prefix);
            generateBenchmarkSupplier(functionFqn, supplierFqn, info);
        });
    }

    private void generateBenchmarkSupplier(String functionFqn, String supplierFqn, BenchmarkInfo benchmarkInfo)
    {
        try (final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(supplierFqn)
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build()
        )
        {
            supplier.addAnnotation(ApplicationScoped.class);
            try (final MethodCreator getMethod = supplier.getMethodCreator("get", BiFunction.class))
            {
                final ResultHandle functionInstance = getMethod.newInstance(MethodDescriptor.ofConstructor(functionFqn));
                getMethod.returnValue(functionInstance);
            }
            try(final MethodCreator newInstanceMethod = supplier.getMethodCreator("newInstance", Object.class))
            {
                final ResultHandle benchmarkInstance = newInstanceMethod.newInstance(MethodDescriptor.ofConstructor(benchmarkInfo.generatedClassQName));
                newInstanceMethod.returnValue(benchmarkInstance);
            }
        }
    }

    private void addMethods(Mode benchmarkKind, BenchmarkInfo benchmarkInfo, ClassCreator classCreator)
    {
        // todo support remaining modes
        switch (benchmarkKind)
        {
            case Throughput:
            case AverageTime:
                addThroughputOrAverage(benchmarkKind, benchmarkInfo, classCreator);
                break;
            // todo SampleTime
            // case SampleTime:
            //    generateSampleTime(writer, benchmarkKind, methodGroup, states);
            //    break;
            // todo SingleShotTime
            // case SingleShotTime:
            //     generateSingleShotTime(writer, benchmarkKind, methodGroup, states);
            //     break;
            default:
                // todo what to do?
                // throw new AssertionError("Shouldn't be here");
        }
    }

    private void addThroughputOrAverage(Mode benchmarkKind, BenchmarkInfo benchmarkInfo, ClassCreator classCreator)
    {
        // todo sub groups
        // final boolean isSingleMethod = (methodGroup.methods().size() == 1);
        // int subGroup = -1;
        for (MethodInfo method : benchmarkInfo.methodGroup.methods())
        {
            // todo sub groups
            // subGroup++;

            final JandexMethodInfo jandexMethod = (JandexMethodInfo) method;

            // Calculating stub
            final MethodDescriptor stubMethod = addThroughputOrAverageStub(jandexMethod, benchmarkKind, classCreator, benchmarkInfo);

            // Function implementation bringing it all together
            addApply(stubMethod, classCreator);
        }
    }

    private static void addApply(
        MethodDescriptor methodDesc
        , ClassCreator classCreator
    )
    {
        try (final MethodCreator apply = classCreator.getMethodCreator("apply", Object.class, Object.class, Object.class))
        {
            final ResultHandle control = apply.getMethodParam(0);
            final ResultHandle workerData = apply.getMethodParam(1);
            final ResultHandle taskResult = apply.invokeVirtualMethod(methodDesc, apply.getThis(), control, workerData);
            apply.returnValue(taskResult);
        }
    }

    private Multimap<ClassInfo, MethodInfo> buildAnnotatedSet_(GeneratorSource source)
    {
        return reflection.invoke("buildAnnotatedSet", source, GeneratorSource.class);
    }

    private Collection<BenchmarkInfo> makeBenchmarkInfo_(ClassInfo clazz, Collection<MethodInfo> methods)
    {
        return reflection.invoke("makeBenchmarkInfo", clazz, ClassInfo.class, methods, Collection.class);
    }

    private static MethodDescriptor addThroughputOrAverageStub(
        JandexMethodInfo methodInfo
        , Mode benchmarkKind
        , ClassCreator function
        , BenchmarkInfo benchmarkInfo
    )
    {
        String methodName = methodInfo.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX;

        final List<String> paramNames = new ArrayList<>();
        paramNames.add(InfraControl.class.getName());
        paramNames.add("org.mendrugo.fibula.runner.WorkerData");

        try (final MethodCreator stub = function.getMethodCreator(methodName, BenchmarkTaskResult.class.getName(), paramNames.toArray(new String[0])))
        {
            final ResultHandle control = stub.getMethodParam(0);
            final ResultHandle workerData = stub.getMethodParam(1);

            final ResultHandle instance = stub.readInstanceField(FieldDescriptor.of("org.mendrugo.fibula.runner.WorkerData", "instance", Object.class.getName()), workerData);
            final ResultHandle threadParams = stub.readInstanceField(FieldDescriptor.of("org.mendrugo.fibula.runner.WorkerData", "threadParams", ThreadParams.class.getName()), workerData);

            final MethodDescriptor method = MethodDescriptor.ofMethod(
                benchmarkInfo.generatedClassQName
                , methodInfo.getName() + "_" + benchmarkKind.name()
                , BenchmarkTaskResult.class.getName()
                , InfraControl.class.getName()
                , ThreadParams.class.getName()
            );
            final ResultHandle taskResult = stub.invokeVirtualMethod(method, instance, control, threadParams);
            stub.returnValue(taskResult);
            return stub.getMethodDescriptor();
        }
    }
}
