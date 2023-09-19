package org.mendrugo.fibula.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
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

    @BuildStep
    @Record(STATIC_INIT)
    void scanForBenchmarks(CombinedIndexBuildItem index, FibulaRecorder recorder)
    {
        for (AnnotationInstance ann : index.getIndex().getAnnotations(NATIVE_BENCHMARK))
        {
            final MethodInfo methodInfo = ann.target().asMethod();
            System.out.println(methodInfo);
            recorder.log(methodInfo.name());
        }

//        IndexView indexView = beanArchiveIndex.getIndex();
//        Collection<AnnotationInstance> testBeans = indexView.getAnnotations(TEST_ANNOTATION);
//        for (AnnotationInstance ann : testBeans) {
//            ClassInfo beanClassInfo = ann.target().asClass();
//            try {
//                boolean isConfigConsumer = beanClassInfo.interfaceNames()
//                    .stream()
//                    .anyMatch(dotName -> dotName.equals(DotName.createSimple(IConfigConsumer.class.getName())));
//                if (isConfigConsumer) {
//                    Class<IConfigConsumer> beanClass = (Class<IConfigConsumer>) Class.forName(beanClassInfo.name().toString(), false, Thread.currentThread().getContextClassLoader());
//                    testBeanProducer.produce(new TestBeanBuildItem(beanClass));
//                    log.infof("Configured bean: %s", beanClass);
//                }
//            } catch (ClassNotFoundException e) {
//                log.warn("Failed to load bean class", e);
//            }
//        }
    }

    @BuildStep
    FeatureBuildItem feature()
    {
        return new FeatureBuildItem(FEATURE);
    }
}