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

import java.util.function.Function;

class FibulaProcessor
{
    private static final String FEATURE = "fibula-extension";
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";
    private static final DotName NATIVE_BENCHMARK = DotName.createSimple(Benchmark.class.getName());

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
        for (AnnotationInstance ann : index.getIndex().getAnnotations(NATIVE_BENCHMARK))
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
        final ClassCreator supplier = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format(
                "%s.%s_%s_Supplier"
                , PACKAGE_NAME
                , classInfo.simpleName()
                , methodInfo.name())
            )
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build();
        supplier.addAnnotation(ApplicationScoped.class);

        final MethodCreator run = supplier.getMethodCreator("get", Function.class);
        final ResultHandle newInstance = run.newInstance(MethodDescriptor.ofConstructor(functionClassName));
        run.returnValue(newInstance);
        run.close();

        supplier.close();
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

