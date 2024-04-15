package org.mendrugo.fibula.extension.deployment;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.logging.Log;
import org.jboss.jandex.DotName;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.generators.core.JmhBenchmarkGenerator;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;

class BenchmarkProcessor
{
    private static final String FEATURE = "fibula-extension";
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";

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
        final ClassOutput beanOutput = new GeneratedBeanGizmoAdaptor(generatedBeanClasses);
        final ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);

        final JandexGeneratorSource source = new JandexGeneratorSource(index.getIndex());
        final JmhBenchmarkGenerator generator = new JmhBenchmarkGenerator(beanOutput, classOutput);
        generator.generate(source);
        generator.complete(buildSystemTarget);

        Log.info("Generating blackhole substitution");
        generateBlackholeSubstitution(classOutput);
    }

    private void generateBlackholeSubstitution(ClassOutput classOutput)
    {
        final String className = String.format(
            "%s.Target_org_openjdk_jmh_infra_Blackhole"
            , PACKAGE_NAME
        );

        try (final ClassCreator blackhole = ClassCreator.builder()
            .classOutput(classOutput)
            .className(className)
            .setFinal(true)
            .build()
        )
        {
            blackhole.addAnnotation(TargetClass.class).add("className", "org.openjdk.jmh.infra.Blackhole");

            final String graalCompilerPackagePrefix = System.getProperty("fibula.graal.compiler.package.prefix", "org.graalvm");

            List<Class<?>> consumeParameterTypes = List.of(
                byte.class
                , boolean.class
                , char.class
                , double.class
                , float.class
                , int.class
                , long.class
                , short.class
                , Object.class
            );
            consumeParameterTypes.forEach(clazz -> generateBlackholeConsumeSubstitution(clazz, graalCompilerPackagePrefix, blackhole));
        }
    }

    private static void generateBlackholeConsumeSubstitution(Class<?> clazz, String graalCompilerPackagePrefix, ClassCreator blackhole)
    {
        try (final MethodCreator consume = blackhole.getMethodCreator("consume", void.class, clazz))
        {
            consume.addAnnotation(Substitute.class);

            final ResultHandle value = consume.getMethodParam(0);
            consume.invokeStaticMethod(MethodDescriptor.ofMethod(graalCompilerPackagePrefix + ".compiler.api.directives.GraalDirectives", "blackhole", void.class, clazz), value);
            consume.returnVoid();
        }
    }
}

