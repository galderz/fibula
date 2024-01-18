package org.mendrugo.fibula.extension.deployment;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
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
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;
import org.openjdk.jmh.util.lines.TestLineWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

class FibulaProcessor
{
    private static final String FEATURE = "fibula-extension";
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";
    private static final DotName BENCHMARK = DotName.createSimple(Benchmark.class.getName());

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
        System.out.println("Generating command...");

        final List<MethodInfo> methods = index.getIndex().getAnnotations(BENCHMARK).stream()
            .map(annotation -> annotation.target().asMethod())
            .filter(FibulaProcessor::isSupportedBenchmark)
            .toList();

        generateBenchmarkList(methods, buildSystemTarget);
        generateBenchmarkClasses(methods, generatedBeanClasses);
    }

    private static boolean isSupportedBenchmark(MethodInfo methodInfo)
    {
        return methodInfo.declaringClass().simpleName().contains("JMHSample_01")
            || methodInfo.declaringClass().simpleName().contains("FibulaSample_01");
    }

    private void generateBenchmarkClasses(List<MethodInfo> methods, BuildProducer<GeneratedBeanBuildItem> generatedBeans)
    {
        final ClassOutput beanOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        methods.forEach(m -> generateBenchmarkBytecode(m, beanOutput));
    }

    private void generateBenchmarkList(List<MethodInfo> methods, BuildSystemTargetBuildItem buildSystemTarget)
    {
        final File resourceDir = buildSystemTarget.getOutputDirectory().resolve(Path.of("classes")).toFile();
        final FileSystemDestination destination = new FileSystemDestination(resourceDir, null);
        try (OutputStream stream = destination.newResource(BenchmarkList.BENCHMARK_LIST.substring(1)))
        {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8)))
            {
                methods.forEach(method -> writeAnnotationParams(method, writer));
            }
        } catch (IOException ex) {
            destination.printError("Error writing benchmark list", ex);
        }
    }

    private void generateBenchmarkBytecode(
        MethodInfo method
        , ClassOutput beanOutput
    )
    {
        final ClassInfo classInfo = method.declaringClass();
        final String functionClassName = generateFunction(method, beanOutput);
        final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format(
                "%s.%s_%s_Throughput_Supplier"
                , PACKAGE_NAME
                , classInfo.simpleName()
                , method.name()
            ))
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build();
        supplier.addAnnotation(ApplicationScoped.class);

        final MethodCreator get = supplier.getMethodCreator("get", Function.class);
        final ResultHandle newInstance = get.newInstance(MethodDescriptor.ofConstructor(functionClassName));
        get.returnValue(newInstance);
        get.close();

        supplier.close();
    }

    private static void writeAnnotationParams(MethodInfo method, PrintWriter writer)
    {
        final ClassInfo classInfo = method.declaringClass();
        final String params = generateAnnotationParams(classInfo.name().toString(), classInfo.simpleName(), method.name());
        writer.println(params);
    }

    private static String generateAnnotationParams(String className, String supplierPrefix, String method)
    {
        final Mode mode = Mode.Throughput;
        final int[] threadGroups = new int[]{1};
        final Optional<Collection<String>> threadGroupLabels = Optional.none();
        final Optional<Integer> threads = Optional.none();
        final Optional<Integer> warmupIterations = Optional.none();
        final Optional<TimeValue> warmupTime = Optional.none();
        final Optional<Integer> warmupBatchSize = Optional.none();
        final Optional<Integer> measurementIterations = Optional.none();
        final Optional<TimeValue> measurementTime = Optional.none();
        final Optional<Integer> measurementBatchSize = Optional.none();
        final Optional<Integer> forks = Optional.none();
        final Optional<Integer> warmupForks = Optional.none();
        final Optional<String> jvm = Optional.none();
        final Optional<Collection<String>> jvmArgs = Optional.none();
        final Optional<Collection<String>> jvmArgsPrepend = Optional.none();
        final Optional<Collection<String>> jvmArgsAppend = Optional.none();
        final Optional<Map<String, String[]>> params = Optional.none();
        final Optional<TimeUnit> tu = Optional.none();
        final Optional<Integer> opsPerInvocation = Optional.none();
        final Optional<TimeValue> timeout = Optional.none();

        // JMH
        TestLineWriter writer = new TestLineWriter();
        writer.putString(className);
        writer.putString(supplierPrefix);
        writer.putString(method);
        writer.putString(mode.toString());
        writer.putOptionalInt(threads);
        writer.putIntArray(threadGroups);
        writer.putOptionalStringCollection(threadGroupLabels);
        writer.putOptionalInt(warmupIterations);
        writer.putOptionalTimeValue(warmupTime);
        writer.putOptionalInt(warmupBatchSize);
        writer.putOptionalInt(measurementIterations);
        writer.putOptionalTimeValue(measurementTime);
        writer.putOptionalInt(measurementBatchSize);
        writer.putOptionalInt(forks);
        writer.putOptionalInt(warmupForks);
        writer.putOptionalString(jvm);
        writer.putOptionalStringCollection(jvmArgs);
        writer.putOptionalStringCollection(jvmArgsPrepend);
        writer.putOptionalStringCollection(jvmArgsAppend);
        writer.putOptionalParamCollection(params);
        writer.putOptionalTimeUnit(tu);
        writer.putOptionalInt(opsPerInvocation);
        writer.putOptionalTimeValue(timeout);

        return writer.toString();
    }

    // todo use generate class build item, not a generate bean class build item
    private String generateFunction(MethodInfo methodInfo, ClassOutput beanOutput)
    {
        final ClassInfo classInfo = methodInfo.declaringClass();

        final String className = String.format(
            "%s.%s_%s_Throughput_Function"
            , PACKAGE_NAME
            , classInfo.simpleName()
            , methodInfo.name());

        final ClassCreator function = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(className)
            .interfaces(Function.class)
            .build();

        final MethodCreator apply = function.getMethodCreator("apply", Object.class, Object.class);
        // final RawResults raw = new RawResults();
        final AssignableResultHandle raw = apply.createVariable(RawResults.class);
        apply.assign(raw, apply.newInstance(MethodDescriptor.ofConstructor(RawResults.class)));
        // raw.startTime = System.nanoTime();
        final ResultHandle startTime = apply.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
        apply.writeInstanceField(FieldDescriptor.of(RawResults.class, "startTime", long.class), raw, startTime);
        // long operations = 0;
        final AssignableResultHandle operations = apply.createVariable(long.class);
        apply.assign(operations, apply.load(0L));
        // Create instance of benchmark
        final String typeName = classInfo.name().toString();
        final String typeDescriptor = "L" + typeName.replace('.', '/') + ";";
        final AssignableResultHandle benchmark = apply.createVariable(typeDescriptor);
        apply.assign(benchmark, apply.newInstance(MethodDescriptor.ofConstructor(classInfo.name().toString())));
        // Loop
        final WhileLoop whileLoop = apply.whileLoop(bc -> bc.ifFalse(
            bc.readInstanceField(FieldDescriptor.of("org.mendrugo.fibula.runner.Infrastructure", "isDone", boolean.class), bc.getMethodParam(0)) // todo share class
        ));
        final BytecodeCreator whileLoopBlock = whileLoop.block();
        whileLoopBlock.invokeVirtualMethod(MethodDescriptor.of(methodInfo), benchmark);
        whileLoopBlock.assign(operations, whileLoopBlock.add(operations, whileLoopBlock.load(1L)));
        whileLoopBlock.close();
        // raw.stopTime = System.nanoTime();
        final ResultHandle stopTime = apply.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "nanoTime", long.class));
        apply.writeInstanceField(FieldDescriptor.of(RawResults.class, "stopTime", long.class), raw, stopTime);
        // JmhRawResults.setMeasureOps(operations, raw);
        apply.invokeStaticMethod(MethodDescriptor.ofMethod("org.mendrugo.fibula.runner.JmhRawResults", "setMeasuredOps", void.class, long.class, RawResults.class), operations, raw); // todo share class
        // return raw;
        apply.returnValue(raw);
        apply.close();

        function.close();
        return className;
    }
}

