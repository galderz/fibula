package org.mendrugo.fibula.extension.deployment;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.gizmo.ClassOutput;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.mendrugo.fibula.annotations.NativeBenchmark;
import org.mendrugo.fibula.extension.runtime.FibulaRecorder;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

class FibulaExtensionProcessor
{
    private static final String FEATURE = "fibula-extension";

    private static DotName NATIVE_BENCHMARK = DotName.createSimple(NativeBenchmark.class.getName());

//    @BuildStep
//    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency)
//    {
//        indexDependency.produce(new IndexDependencyBuildItem("org.mendrugo.fibula", "fibula-core"));
//    }

    @BuildStep
    void scanForBenchmarks(
        BuildProducer<GeneratedClassBuildItem> generatedClasses
        , BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses
        , CombinedIndexBuildItem index
    )
    {
        final BenchmarkInfo.Builder benchBuilder = new BenchmarkInfo.Builder();
        for (AnnotationInstance ann : index.getIndex().getAnnotations(NATIVE_BENCHMARK))
        {
            final MethodInfo methodInfo = ann.target().asMethod();
            System.out.println(methodInfo);
            benchBuilder.withMethod(methodInfo);
        }

        // ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        ClassOutput beanOutput = new GeneratedBeanGizmoAdaptor(generatedBeanClasses);
        new BenchmarkGenerator().generate(benchBuilder.build(), classOutput, beanOutput);
        System.out.println("Generated: " + classOutput);
    }

    @BuildStep
    FeatureBuildItem feature()
    {
        return new FeatureBuildItem(FEATURE);
    }
}
