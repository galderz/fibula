package org.mendrugo.fibula.extension.deployment;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.gizmo.WhileLoop;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.mendrugo.fibula.results.Infrastructure;
import org.mendrugo.fibula.results.JmhRawResults;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.runner.BenchmarkList;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

class BenchmarkProcessor
{
    private static final String FEATURE = "fibula-extension";
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";
    private static final DotName BENCHMARK = DotName.createSimple(Benchmark.class.getName());
    private static final DotName BENCHMARK_MODE = DotName.createSimple(BenchmarkMode.class.getName());
    private static final DotName STATE = DotName.createSimple(State.class.getName());

    final Identifiers identifiers = new Identifiers(); // todo reuse

    @BuildStep
    FeatureBuildItem feature()
    {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void generateCommand(
        BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses
        , BuildProducer<GeneratedClassBuildItem> generatedClasses
        , CombinedIndexBuildItem index
        , BuildSystemTargetBuildItem buildSystemTarget
    )
    {
        Log.info("Generating benchmark bytecode");

        final List<MethodInfo> methods = index.getIndex().getAnnotations(BENCHMARK).stream()
            .map(annotation -> annotation.target().asMethod())
            .filter(BenchmarkProcessor::isSupportedBenchmark)
            .toList();

        generateBenchmarkList(methods, buildSystemTarget);
        generateBenchmarkClasses(methods, generatedBeanClasses, generatedClasses, index.getIndex());
    }

    private static boolean isSupportedBenchmark(MethodInfo methodInfo)
    {
        return methodInfo.declaringClass().simpleName().contains("JMHSample_01")
            || methodInfo.declaringClass().simpleName().contains("JMHSample_03")
            || methodInfo.declaringClass().simpleName().contains("FibulaSample_01")
            || methodInfo.declaringClass().simpleName().contains("FibulaSample_02");
    }

    private void generateBenchmarkClasses(
        List<MethodInfo> methods
        , BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses
        , BuildProducer<GeneratedClassBuildItem> generatedClasses
        , IndexView index
    )
    {
        final ClassOutput beanOutput = new GeneratedBeanGizmoAdaptor(generatedBeanClasses);
        final GeneratedClassGizmoAdaptor classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        // todo consider combining method and mode into a local benchmark info or equivalent
        for (MethodInfo method : methods)
        {
            for (Mode mode : benchmarkModes(method))
            {
                final String functionFqn = generateBenchmarkFunction(method, mode, classOutput, index);
                generateBenchmarkSupplier(method, mode, beanOutput, functionFqn);
            }
        }
    }

    private void generateBenchmarkList(List<MethodInfo> methods, BuildSystemTargetBuildItem buildSystemTarget)
    {
        final File resourceDir = buildSystemTarget.getOutputDirectory().resolve(Path.of("classes")).toFile();
        final FileSystemDestination destination = new FileSystemDestination(resourceDir, null);
        try (OutputStream stream = destination.newResource(BenchmarkList.BENCHMARK_LIST.substring(1)))
        {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8)))
            {
                // todo consider combining method and mode into a local benchmark info or equivalent
                for (MethodInfo method : methods)
                {
                    benchmarkModes(method)
                        .forEach(mode -> writer.println(JmhParameters.asLine(method, mode)));
                }
            }
        } catch (IOException ex) {
            destination.printError("Error writing benchmark list", ex);
        }
    }

    private void generateBenchmarkSupplier(MethodInfo method, Mode mode, ClassOutput beanOutput, String functionFqn)
    {
        final ClassInfo classInfo = method.declaringClass();
        try (final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format(
                "%s.%s_%s_%s_Supplier"
                , PACKAGE_NAME
                , classInfo.simpleName() // todo include package name here
                , method.name()
                , mode.name()
            ))
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

    private static List<Mode> benchmarkModes(MethodInfo method)
    {
        final AnnotationInstance annotation = method.annotation(BENCHMARK_MODE);
        if (annotation != null)
        {
            return Arrays.stream(annotation.value().asEnumArray())
                .map(Mode::valueOf)
                .toList();
        }

        return List.of(Mode.Throughput);
    }

    private String generateBenchmarkFunction(MethodInfo methodInfo, Mode mode, ClassOutput classOutput, IndexView index)
    {
        final ClassInfo classInfo = methodInfo.declaringClass();

        final String className = String.format(
            "%s.%s_%s_%s_Function"
            , PACKAGE_NAME
            , classInfo.simpleName()
            , methodInfo.name()
            , mode.name()
        );

        final ClassCreator function = ClassCreator.builder()
            .classOutput(classOutput)
            .className(className)
            .interfaces(Function.class)
            .build();

        // Benchmark field and initialization
        final MethodDescriptor tryInitMethod = generateUnsharedState(methodInfo.declaringClass().name().toString(), function);

        // State field and initialization
        final List<MethodDescriptor> paramInitMethods = methodInfo.parameters().stream()
            .map(paramInfo -> generateScopeState(paramInfo, function, index))
            .toList();

        final List<String> paramNames = methodInfo.parameters().stream()
            .map(paramInfo -> paramInfo.type().name().toString())
            .toList();

        // Calculating stub
        final MethodDescriptor stubMethod = generateThroughputOrAverageStub(methodInfo, mode, paramNames, function);

        // Function implementation bringing it all together
        generateApply(stubMethod, tryInitMethod, methodInfo, paramInitMethods, function);

        function.close();
        return className;
    }

    private MethodDescriptor generateScopeState(MethodParameterInfo paramInfo, ClassCreator function, IndexView index)
    {
        final Optional<AnnotationInstance> stateAnnotation = index.getAnnotations(STATE).stream()
            .filter(annotation -> annotation.target().asClass().name().equals(paramInfo.type().name()))
            .findFirst();

        if (stateAnnotation.isPresent())
        {
            final Scope scope = Scope.valueOf(stateAnnotation.get().value().asEnum());
            return switch (scope)
            {
                case Benchmark -> generateSharedState(paramInfo, function);
                case Thread -> generateUnsharedState(paramInfo.type().name().toString(), function);
                default -> throw new RuntimeException("NYI");
            };
        }
        throw new IllegalStateException("Parameter type has no @State annotation");
    }

    private MethodDescriptor generateUnsharedState(String stateFqn, ClassCreator function)
    {
        // Unshared f_unshared;
        final String fieldIdentifier = "f_" + identifiers.collapseTypeName(stateFqn) + identifiers.identifier(Scope.Thread);
        final FieldDescriptor field = function.getFieldCreator(fieldIdentifier, DescriptorUtils.extToInt(stateFqn)).getFieldDescriptor();

        final String methodName = "_fib_tryInit_" + field.getName();
        try (final MethodCreator tryInit = function.getMethodCreator(methodName, field.getType()))
        {
            // Unshared val = this.f_unshared;
            final AssignableResultHandle val = tryInit.createVariable(field.getType());
            tryInit.assign(val, tryInit.readInstanceField(field, tryInit.getThis()));

            // if (val == null) {
            try (final BytecodeCreator trueBranch = tryInit.ifReferencesEqual(val, tryInit.loadNull()).trueBranch())
            {
                // val = new Unshared();
                trueBranch.assign(val, trueBranch.newInstance(MethodDescriptor.ofConstructor(stateFqn)));
                // this.f_unshared = val;
                trueBranch.writeInstanceField(field, trueBranch.getThis(), val);
            }
            // }

            // return val
            tryInit.returnValue(val);
            return tryInit.getMethodDescriptor();
        }
    }

    private MethodDescriptor generateSharedState(MethodParameterInfo paramInfo, ClassCreator classCreator)
    {
        final String paramFqn = paramInfo.type().name().toString();

        // static final ReentrantLock f_lock = new ReentrantLock();
        final FieldDescriptor lockField = initStateLock(classCreator);

        // static volatile S f_state;
        final String stateFieldIdentifier = "f_" + identifiers.collapseTypeName(paramFqn) + identifiers.identifier(Scope.Thread);
        final FieldDescriptor stateField = classCreator.getFieldCreator(stateFieldIdentifier, DescriptorUtils.extToInt(paramFqn))
            .setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE)
            .getFieldDescriptor();

        final String methodName = "_fib_tryInit_" + stateField.getName();
        try (final MethodCreator tryInit = classCreator.getMethodCreator(methodName, stateField.getType()))
        {
            // S val = f_state;
            // if (val != null) return val;
            final AssignableResultHandle val = tryInit.createVariable(stateField.getType());
            readStaticFieldAndReturnIfNotNull(val, stateField, tryInit);

            // f_lock.lock()
            final ResultHandle lock = tryInit.readStaticField(lockField);
            tryInit.invokeVirtualMethod(MethodDescriptor.ofMethod(ReentrantLock.class, "lock", void.class), lock);

            // try {
            try (final TryBlock tryBlock = tryInit.tryBlock())
            {
                // val = f_state;
                // if (val != null) return val;
                readStaticFieldAndReturnIfNotNull(val, stateField, tryBlock);

                // val = new F();
                final ResultHandle newVal = tryBlock.newInstance(MethodDescriptor.ofConstructor(paramFqn));
                tryBlock.assign(val, newVal);
                // f_state = val;
                tryBlock.writeStaticField(stateField, val);

                // f_lock.unlock()
                tryBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(ReentrantLock.class, "unlock", void.class), lock);

                // } catch (Throwable) {
                try (final CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class))
                {
                    // f_lock.unlock()
                    catchBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(ReentrantLock.class, "unlock", void.class), lock);
                }
                // }
            }

            tryInit.returnValue(val);
            return tryInit.getMethodDescriptor();
        }
    }

    private FieldDescriptor initStateLock(ClassCreator classCreator)
    {
        final String lockFieldIdentifier = "f_" + identifiers.collapseTypeName(ReentrantLock.class.getName()) + identifiers.identifier(Scope.Benchmark);
        final FieldDescriptor lockField = classCreator.getFieldCreator(lockFieldIdentifier, DescriptorUtils.extToInt(ReentrantLock.class.getName()))
            .setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
            .getFieldDescriptor();

        // static {
        try (MethodCreator staticInit = classCreator.getMethodCreator("<clinit>", void.class))
        {
            staticInit.setModifiers(ACC_STATIC);
            ResultHandle lockInstance = staticInit.newInstance(MethodDescriptor.ofConstructor(ReentrantLock.class));
            // f_lock = new ReentrantLock();
            staticInit.writeStaticField(lockField, lockInstance);
            staticInit.returnVoid();
        }
        return lockField;
    }

    private void readStaticFieldAndReturnIfNotNull(AssignableResultHandle val, FieldDescriptor field, BytecodeCreator method)
    {
        // F val = this.f;
        method.assign(val, method.readStaticField(field));

        // if (val != null) {
        try (final BytecodeCreator trueBranch = method.ifReferencesNotEqual(val, method.loadNull()).trueBranch())
        {
            // return val
            trueBranch.returnValue(val);
        }
        // }
    }

    private static void generateApply(
        MethodDescriptor stubMethod
        , MethodDescriptor tryInitMethod
        , MethodInfo methodInfo
        , List<MethodDescriptor> paramInitMethods
        , ClassCreator function
    )
    {
        final List<ResultHandle> stubParameters = new ArrayList<>();

        final ClassInfo classInfo = methodInfo.declaringClass();
        try (final MethodCreator apply = function.getMethodCreator("apply", Object.class, Object.class))
        {
            final ResultHandle infrastructure = apply.getMethodParam(0);
            stubParameters.add(infrastructure);

            // RawResults raw = new RawResults();
            final AssignableResultHandle raw = apply.createVariable(RawResults.class);
            apply.assign(raw, apply.newInstance(MethodDescriptor.ofConstructor(RawResults.class)));
            stubParameters.add(raw);

            // B benchmark = tryInit();
            final String userType = classInfo.name().toString();
            final AssignableResultHandle benchmark = apply.createVariable(DescriptorUtils.extToInt(userType));
            apply.assign(benchmark, apply.invokeVirtualMethod(tryInitMethod, apply.getThis()));
            stubParameters.add(benchmark);

            paramInitMethods.stream()
                .map(initMethod -> apply.invokeVirtualMethod(initMethod, apply.getThis()))
                .forEach(stubParameters::add);

            // stub(infrastructure, raw, benchmark...);
            apply.invokeVirtualMethod(stubMethod, apply.getThis(), stubParameters.toArray(new ResultHandle[0]));

            // return raw;
            apply.returnValue(raw);
        }
    }

    private static MethodDescriptor generateThroughputOrAverageStub(MethodInfo methodInfo, Mode mode, List<String> stateParamNames, ClassCreator function)
    {
        final ClassInfo classInfo = methodInfo.declaringClass();
        final String stubMethodName = mode.shortLabel() + "_fibStub";

        final List<String> paramNames = new ArrayList<>();
        paramNames.add("org.mendrugo.fibula.results.Infrastructure");
        paramNames.add("org.openjdk.jmh.results.RawResults");
        paramNames.add(classInfo.name().toString());
        paramNames.addAll(stateParamNames);

        final MethodCreator stub = function.getMethodCreator(
            stubMethodName
            , "void"
            , paramNames.toArray(new String[0])
        );
        final ResultHandle infrastructure = stub.getMethodParam(0);
        final ResultHandle raw = stub.getMethodParam(1);
        final ResultHandle benchmark = stub.getMethodParam(2);
        final List<ResultHandle> params = IntStream.range(0, stateParamNames.size())
            .mapToObj(i -> stub.getMethodParam(i + 3))
            .toList();

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
        final BytecodeCreator whileLoopBlock = whileLoop.block();
        whileLoopBlock.invokeVirtualMethod(MethodDescriptor.of(methodInfo), benchmark, params.toArray(new ResultHandle[0]));
        whileLoopBlock.assign(operations, whileLoopBlock.add(operations, whileLoopBlock.load(1L)));
        whileLoopBlock.close();

        // raw.stopTime = System.nanoTime();
        final ResultHandle stopTime = stub.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
        stub.writeInstanceField(FieldDescriptor.of(RawResults.class, "stopTime", long.class), raw, stopTime);

        // JmhRawResults.setMeasureOps(operations, raw);
        stub.invokeStaticMethod(MethodDescriptor.ofMethod(JmhRawResults.class, "setMeasuredOps", void.class, long.class, RawResults.class), operations, raw);

        stub.returnVoid();
        stub.close();
        return stub.getMethodDescriptor();
    }
}

