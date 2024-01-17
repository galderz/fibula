package org.mendrugo.fibula.extension.deployment;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.FileSystemDestination;
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
        MethodInfo methodInfo
        , ClassOutput beanOutput
    )
    {
        final String userClassQName = getUserClassQName(methodInfo);
        final String functionClassName = generateFunction(methodInfo, beanOutput);
        final String method = methodInfo.name();

        final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format(
                "%s_%s_Supplier"
                , userClassQName
                , method
            ))
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build();
        supplier.addAnnotation(ApplicationScoped.class);

        final MethodCreator get = supplier.getMethodCreator("get", Function.class);
        final ResultHandle newInstance = get.newInstance(MethodDescriptor.ofConstructor(functionClassName));
        get.returnValue(newInstance);
        get.close();

//        final MethodCreator benchmark = supplier.getMethodCreator("benchmark", String.class);
//        final String params = generateAnnotationParams(userClassQName, method);
//        benchmark.returnValue(benchmark.load(params));
//        benchmark.close();

        supplier.close();
    }

    private static String getUserClassQName(MethodInfo methodInfo)
    {
        final ClassInfo classInfo = methodInfo.declaringClass();
        return String.format("%s.%s", PACKAGE_NAME, classInfo.simpleName());
    }

    private static void writeAnnotationParams(MethodInfo method, PrintWriter writer)
    {
        final String userClassQName = getUserClassQName(method);
        final String params = generateAnnotationParams(userClassQName, method.name());
        writer.println(params);
    }

    private static String generateAnnotationParams(String userClassQName, String method)
    {
        final String generatedClassQName = "UNUSED";
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
        writer.putString(userClassQName);
        writer.putString(generatedClassQName);
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

    private String generateFunction(MethodInfo methodInfo, ClassOutput beanOutput)
    {
        final ClassInfo classInfo = methodInfo.declaringClass();

        final String className = String.format(
            "%s.%s_%s_Function"
            , PACKAGE_NAME
            , classInfo.simpleName()
            , methodInfo.name());

        final ClassCreator function = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(className)
            .superClass("org.mendrugo.fibula.runner.ThroughputFunction") // todo share class
            .build();

        final MethodCreator run = function.getMethodCreator("doOperation", void.class);
        final String typeName = classInfo.name().toString();
        final String typeDescriptor = "L" + typeName.replace('.', '/') + ";";
        final AssignableResultHandle variable = run.createVariable(typeDescriptor);
        run.assign(variable, run.newInstance(MethodDescriptor.ofConstructor(classInfo.name().toString())));
        run.invokeVirtualMethod(MethodDescriptor.of(methodInfo), variable);
        run.returnValue(null);
        run.close();

        function.close();
        return className;
    }
}

