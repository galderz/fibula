package org.mendrugo.fibula.extension.deployment;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;
import org.openjdk.jmh.util.lines.TestLineWriter;

import java.util.Collection;
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
    )
    {
        System.out.println("Generating command...");

        BenchmarkInfo.Builder builder = new BenchmarkInfo.Builder();
        for (AnnotationInstance ann : index.getIndex().getAnnotations(BENCHMARK))
        {
            MethodInfo methodInfo = ann.target().asMethod();
            builder.withMethod(methodInfo);
            System.out.println(methodInfo);
        }

        final ClassOutput beanOutput = new GeneratedBeanGizmoAdaptor(generatedBeanClasses);
        final BenchmarkInfo build = builder.build();
        if (build.methods().isEmpty())
        {
            return;
        }

        build.methods().forEach(m -> generate(m, beanOutput));
    }

    private void generate(MethodInfo methodInfo, ClassOutput beanOutput)
    {
        final ClassInfo classInfo = methodInfo.declaringClass();

        final String functionClassName = generateFunction(methodInfo, beanOutput);
        generateSupplier(methodInfo, beanOutput, classInfo, functionClassName);
    }

    private static void generateSupplier(MethodInfo methodInfo, ClassOutput beanOutput, ClassInfo classInfo, String functionClassName)
    {
        final String userClassQName = String.format("%s.%s", PACKAGE_NAME, classInfo.simpleName());
        final String method = methodInfo.name();

        final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format(
                "%s_%s_Supplier"
                , userClassQName
                , method)
            )
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build();
        supplier.addAnnotation(ApplicationScoped.class);

        final MethodCreator getMethod = supplier.getMethodCreator("get", Function.class);
        final ResultHandle newInstance = getMethod.newInstance(MethodDescriptor.ofConstructor(functionClassName));
        getMethod.returnValue(newInstance);
        getMethod.close();

        final MethodCreator annotationParams = supplier.getMethodCreator("annotationParams", String.class);
        final String params = generateAnnotationParams(userClassQName, method);
        annotationParams.returnValue(annotationParams.load(params));
        annotationParams.close();

        // todo generate a benchmark list as text and get the supplier to return it
        //      at runtime, the line will be read and deserialized to read the benchmark parameters
        //      e.g. user class name, method name, annotation values...etc
        //      E.g.
        //
        //      Format:
        //
        // TestLineWriter writer = new TestLineWriter();

        supplier.close();
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

