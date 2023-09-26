package org.mendrugo.fibula.extension.deployment;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.mendrugo.fibula.annotations.NativeBenchmark;

import java.util.function.Function;

class FibulaProcessor
{
    private static final String FEATURE = "fibula-extension";
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";
    private static final DotName NATIVE_BENCHMARK = DotName.createSimple(NativeBenchmark.class.getName());

    @BuildStep
    void scanForBenchmarks(
        BuildProducer<GeneratedClassBuildItem> generatedClasses
        , BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses
        , CombinedIndexBuildItem index
    )
    {
        System.out.println("Scan for benchmarks...");
        final BenchmarkInfo.Builder benchmarkBuilder = new BenchmarkInfo.Builder();
        for (AnnotationInstance ann : index.getIndex().getAnnotations(NATIVE_BENCHMARK))
        {
            final MethodInfo methodInfo = ann.target().asMethod();
            System.out.println(methodInfo);
            benchmarkBuilder.withMethod(methodInfo);
        }

        final BenchmarkInfo benchmarkInfo = benchmarkBuilder.build();
        final String functionClassName = generateBenchmarkFunction(benchmarkInfo, new GeneratedBeanGizmoAdaptor(generatedBeanClasses));
        generateBenchmarkSupplier(benchmarkInfo, functionClassName, new GeneratedClassGizmoAdaptor(generatedClasses, true));
    }

    String generateBenchmarkFunction(BenchmarkInfo benchmarkInfo, ClassOutput beanOutput)
    {
        final MethodInfo methodInfo = benchmarkInfo.info();
        final ClassInfo classInfo = methodInfo.declaringClass();

        final String functionClassName = String.format("%s.%s_BenchmarkFunction", PACKAGE_NAME, classInfo.simpleName());

        final ClassCreator function = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(functionClassName)
            .superClass("org.mendrugo.fibula.runner.ThroughputFunction") // todo share class
            .build();

        final MethodCreator doOperation = function.getMethodCreator("doOperation", void.class);
        final String typeName = classInfo.name().toString();
        final String typeDescriptor = "L" + typeName.replace('.', '/') + ";";
        final AssignableResultHandle variable = doOperation.createVariable(typeDescriptor);
        doOperation.assign(variable, doOperation.newInstance(MethodDescriptor.ofConstructor(classInfo.name().toString())));
        doOperation.invokeVirtualMethod(MethodDescriptor.of(methodInfo), variable);
        doOperation.close();
        function.close();

        return functionClassName;
    }

    void generateBenchmarkSupplier(BenchmarkInfo benchmarkInfo, String functionClassName, GeneratedClassGizmoAdaptor beanOutput)
    {
        final MethodInfo methodInfo = benchmarkInfo.info();
        final ClassInfo classInfo = methodInfo.declaringClass();

        final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format("%s.%s_BenchmarkSupplier", PACKAGE_NAME, classInfo.simpleName()))
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build();

        final MethodCreator get = supplier.getMethodCreator("get", Function.class);
        final MethodDescriptor ctor = MethodDescriptor.ofConstructor(functionClassName);
        final ResultHandle function = get.newInstance(ctor);
        get.returnValue(function);

        supplier.close();
    }

    @BuildStep
    FeatureBuildItem feature()
    {
        return new FeatureBuildItem(FEATURE);
    }
}
