package org.openjdk.jmh.generators.core;

import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.WhileLoop;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.DotName;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.mendrugo.fibula.extension.deployment.JandexGeneratorSource;
import org.mendrugo.fibula.extension.deployment.JandexMethodInfo;
import org.mendrugo.fibula.extension.deployment.Reflection;
import org.mendrugo.fibula.results.Infrastructure;
import org.mendrugo.fibula.results.JmhRawResults;
import org.mendrugo.fibula.results.Modes;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.util.Multimap;
import org.openjdk.jmh.util.Optional;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.jboss.jandex.Type.Kind.PRIMITIVE;

public final class JmhBenchmarkGenerator extends BenchmarkGenerator
{
    private static final String JMH_STUB_SUFFIX = "_jmhStub";
    private static final DotName OUTPUT_TIME_UNIT = DotName.createSimple(OutputTimeUnit.class.getName());

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
        final JmhStateObjectHandler states = new JmhStateObjectHandler(compilerControl);

        // bind all methods
        states.bindMethods(classInfo, info.methodGroup);

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

            final String functionFqn = String.format("%s_Function", prefix);

            try (final ClassCreator function = ClassCreator.builder()
                .classOutput(classOutput)
                .className(functionFqn)
                .interfaces(Function.class)
                .build()
            )
            {
                addMethods(benchmarkKind, info.methodGroup, states, function);

                // Write out state initializers
                states.addStateInitializers(function);
            }

            final String supplierFqn = String.format("%s_Supplier", prefix);
            generateBenchmarkSupplier(functionFqn, supplierFqn);
        });
    }

    private void generateBenchmarkSupplier(String functionFqn, String supplierFqn)
    {
        try (final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(supplierFqn)
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build()
        )
        {
            supplier.addAnnotation(ApplicationScoped.class);
            try (final MethodCreator get = supplier.getMethodCreator("get", Function.class))
            {
                final ResultHandle newInstance = get.newInstance(MethodDescriptor.ofConstructor(functionFqn));
                get.returnValue(newInstance);
            }
        }
    }

    private void addMethods(Mode benchmarkKind, MethodGroup methodGroup, JmhStateObjectHandler states, ClassCreator classCreator)
    {
        // todo support remaining modes
        switch (benchmarkKind)
        {
            case Throughput:
            case AverageTime:
                addThroughputOrAverage(benchmarkKind, methodGroup, states, classCreator);
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

    private void addThroughputOrAverage(Mode benchmarkKind, MethodGroup methodGroup, JmhStateObjectHandler states, ClassCreator classCreator)
    {
        // todo sub groups
        // final boolean isSingleMethod = (methodGroup.methods().size() == 1);
        // int subGroup = -1;
        for (MethodInfo method : methodGroup.methods())
        {
            // todo sub groups
            // subGroup++;

            final JandexMethodInfo jandexMethod = (JandexMethodInfo) method;

            // Calculating stub
            final MethodDescriptor stubMethod = addThroughputOrAverageStub(jandexMethod, benchmarkKind, states, classCreator);

            // Function implementation bringing it all together
            addApply(jandexMethod, states, stubMethod, classCreator);
        }
    }

    private static void addApply(
        JandexMethodInfo methodInfo
        , JmhStateObjectHandler states
        , MethodDescriptor methodDesc
        , ClassCreator classCreator
    )
    {
        final List<ResultHandle> stubParameters = new ArrayList<>();

        try (final MethodCreator apply = classCreator.getMethodCreator("apply", Object.class, Object.class))
        {
            final ResultHandle infrastructure = apply.getMethodParam(0);
            stubParameters.add(infrastructure);

            // RawResults raw = new RawResults();
            final AssignableResultHandle raw = apply.createVariable(RawResults.class);
            apply.assign(raw, apply.newInstance(MethodDescriptor.ofConstructor(RawResults.class)));
            stubParameters.add(raw);

            // Blackhole blackhole = new Blackhole(...);
            final AssignableResultHandle blackhole = apply.createVariable(Blackhole.class);
            apply.assign(blackhole, apply.newInstance(
                MethodDescriptor.ofConstructor(Blackhole.class, String.class)
                , apply.load("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")
            ));
            stubParameters.add(blackhole);

            // B benchmark = tryInit();
            // S state = tryInit();
            // ...
            final SequencedMap<StateObject, ResultHandle> stateHandles = states.addStateGetters(methodInfo, apply, classCreator);
            final ResultHandle benchmark = stateHandles.firstEntry().getValue();
            stubParameters.addAll(stateHandles.sequencedValues().reversed());

            // benchmark.setup(); for each iteration setup methods
            iterationProlog(stateHandles, methodInfo, states, apply);

            // stub(infrastructure, raw, benchmark...);
            apply.invokeVirtualMethod(methodDesc, apply.getThis(), stubParameters.toArray(new ResultHandle[0]));

            // benchmark.tearDown(); for each iteration tear down methods
            iterationEpilog(stateHandles, methodInfo, states, apply);

            try (final BytecodeCreator trueBranch = apply
                .ifTrue(apply.readInstanceField(FieldDescriptor.of(Infrastructure.class, "lastIteration", boolean.class), infrastructure))
                .trueBranch()
            ) {
                // benchmark.tearDown(); for each trial tear down methods
                trialEpilog(stateHandles, methodInfo, states, trueBranch);
            }

            // return raw;
            apply.returnValue(raw);
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
        , JmhStateObjectHandler states
        , ClassCreator function
    )
    {
        String methodName = methodInfo.getName() + "_" + benchmarkKind.shortLabel() + JMH_STUB_SUFFIX;

        final List<String> paramNames = new ArrayList<>();
        paramNames.add(Infrastructure.class.getName());
        paramNames.add(RawResults.class.getName());
        paramNames.add(Blackhole.class.getName());
        final LinkedHashSet<StateObject> stateParams = states.stateOrder_(methodInfo, false);
        stateParams.forEach(so -> paramNames.add(so.userType));

        try (final MethodCreator stub = function.getMethodCreator(methodName, "void", paramNames.toArray(new String[0])))
        {
            int paramIndex = 0;
            final ResultHandle infrastructure = stub.getMethodParam(paramIndex++);
            final ResultHandle raw = stub.getMethodParam(paramIndex++);
            final ResultHandle blackhole = stub.getMethodParam(paramIndex++);

            final SequencedMap<StateObject, ResultHandle> stateHandles = new LinkedHashMap<>();
            for (StateObject so : states.stateOrder_(methodInfo, false))
            {
                stateHandles.put(so, stub.getMethodParam(paramIndex++));
            }

            // raw.startTime = System.nanoTime();
            final ResultHandle startTime = stub.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
            stub.writeInstanceField(FieldDescriptor.of(RawResults.class, "startTime", long.class), raw, startTime);

            // long operations = 0;
            final AssignableResultHandle operations = stub.createVariable(long.class);
            stub.assign(operations, stub.load(0L));

            // Loop
            final WhileLoop whileLoop = stub.whileLoop(bc -> bc.ifFalse(
                bc.readInstanceField(FieldDescriptor.of(Infrastructure.class, "isDone", boolean.class), infrastructure)
            ));
            try (final BytecodeCreator whileLoopBlock = whileLoop.block())
            {
                invocationProlog(stateHandles, methodInfo, states, whileLoopBlock);
                emitCall(stateHandles, methodInfo, blackhole, whileLoopBlock);
                invocationEpilog(stateHandles, methodInfo, states, whileLoopBlock);
                whileLoopBlock.assign(operations, whileLoopBlock.add(operations, whileLoopBlock.load(1L)));
            }

            // raw.stopTime = System.nanoTime();
            final ResultHandle stopTime = stub.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
            stub.writeInstanceField(FieldDescriptor.of(RawResults.class, "stopTime", long.class), raw, stopTime);

            // JmhRawResults.setMeasureOps(operations, raw);
            stub.invokeStaticMethod(MethodDescriptor.ofMethod(JmhRawResults.class, "setMeasuredOps", void.class, long.class, RawResults.class), operations, raw);

            stub.returnVoid();
            return stub.getMethodDescriptor();
        }
    }

    private static void invocationProlog(SequencedMap<StateObject, ResultHandle> stateHandles, JandexMethodInfo method, JmhStateObjectHandler states, BytecodeCreator block)
    {
        if (states.hasInvocationStubs(method))
        {
            states.addHelperBlock(method, Level.Invocation, HelperType.SETUP, stateHandles, new ResultHandle[]{}, block);
        }
    }

    private static void invocationEpilog(SequencedMap<StateObject, ResultHandle> stateHandles, JandexMethodInfo method, JmhStateObjectHandler states, BytecodeCreator block)
    {
        if (states.hasInvocationStubs(method))
        {
            states.addHelperBlock(method, Level.Invocation, HelperType.TEARDOWN, stateHandles, new ResultHandle[]{}, block);
        }
    }

    private static void iterationProlog(SequencedMap<StateObject, ResultHandle> stateHandles, JandexMethodInfo method, JmhStateObjectHandler states, BytecodeCreator block)
    {
        if (states.hasInvocationStubs(method))
        {
            states.addHelperBlock(method, Level.Iteration, HelperType.SETUP, stateHandles, new ResultHandle[]{}, block);
        }
    }

    private static void iterationEpilog(SequencedMap<StateObject, ResultHandle> stateHandles, JandexMethodInfo method, JmhStateObjectHandler states, BytecodeCreator block)
    {
        if (states.hasInvocationStubs(method))
        {
            states.addHelperBlock(method, Level.Iteration, HelperType.TEARDOWN, stateHandles, new ResultHandle[]{}, block);
        }
    }

    private static void trialEpilog(SequencedMap<StateObject, ResultHandle> stateHandles, JandexMethodInfo method, JmhStateObjectHandler states, BytecodeCreator block)
    {
        states.addHelperBlock(method, Level.Trial, HelperType.TEARDOWN, stateHandles, new ResultHandle[]{}, block);
    }

    private static void emitCall(SequencedMap<StateObject, ResultHandle> stateHandles, JandexMethodInfo method, ResultHandle blackhole, BytecodeCreator block)
    {
        // Create a copy to remove the benchmark parameter and keep the rest of arguments,
        // but do not the removal affect the original collection
        // final SequencedCollection<ResultHandle> callParams = new ArrayList<>(stateHandles.sequencedValues());
        // final ResultHandle benchmark = callParams.removeLast();

        final SequencedCollection<ResultHandle> candidates = new ArrayList<>(stateHandles.sequencedValues());
        final SequencedCollection<ResultHandle> args = new ArrayList<>();
        for (ParameterInfo paramInfo : method.getParameters())
        {
            final String name = paramInfo.getType().getQualifiedName();
            if (Blackhole.class.getCanonicalName().equals(name))
            {
                args.add(blackhole);
            }
            else
            {
                args.add(candidates.removeFirst());
            }
        }
        final ResultHandle benchmark = candidates.removeLast();

        // benchmark.bench();
        final ResultHandle result = block.invokeVirtualMethod(
            method.getMethodDescriptor()
            , benchmark
            , args.toArray(new ResultHandle[0])
        );
        if (!"void".equalsIgnoreCase(method.getReturnType()))
        {
            block.invokeVirtualMethod(selectBlackholeMethod(method), blackhole, result);
        }
    }

    private static <T> List<T> tail(List<T> list)
    {
        return list.subList(1, list.size());
    }

    private static MethodDescriptor selectBlackholeMethod(JandexMethodInfo methodInfo)
    {
        final Type returnType = methodInfo.getJandexReturnType();
        Class<?> consumeParamClass;
        if (PRIMITIVE == returnType.kind())
        {
            consumeParamClass = switch (((PrimitiveType) returnType).primitive())
            {
                case BYTE -> byte.class;
                case BOOLEAN -> boolean.class;
                case CHAR -> char.class;
                case DOUBLE -> double.class;
                case FLOAT -> float.class;
                case INT -> int.class;
                case LONG -> long.class;
                case SHORT -> short.class;
            };
        }
        else
        {
            consumeParamClass = Object.class;
        }

        return MethodDescriptor.ofMethod(Blackhole.class, "consume", void.class, consumeParamClass);
    }
}
