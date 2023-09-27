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
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.mendrugo.fibula.annotations.NativeBenchmark;

class FibulaProcessor
{
    private static final String FEATURE = "command-extension";
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";
    private static final DotName NATIVE_BENCHMARK = DotName.createSimple(NativeBenchmark.class.getName());

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

        final ClassCreator command = ClassCreator.builder()
            .classOutput(beanOutput)
            .className(String.format(
                "%s.%s_%s_Command"
                , PACKAGE_NAME
                , classInfo.simpleName()
                , methodInfo.name())
            )
            .interfaces("org.mendrugo.fibula.runner.BenchmarkSupplier") // todo share class
            .build();
        command.addAnnotation(ApplicationScoped.class);

        final MethodCreator run = command.getMethodCreator("run", void.class);
        Gizmo.systemOutPrintln(run, run.load("A generated command"));

        final String typeName = classInfo.name().toString();
        final String typeDescriptor = "L" + typeName.replace('.', '/') + ";";
        final AssignableResultHandle variable = run.createVariable(typeDescriptor);
        run.assign(variable, run.newInstance(MethodDescriptor.ofConstructor(classInfo.name().toString())));
        run.invokeVirtualMethod(MethodDescriptor.of(methodInfo), variable);

        run.returnValue(null);
        run.close();

        command.close();
    }
}

