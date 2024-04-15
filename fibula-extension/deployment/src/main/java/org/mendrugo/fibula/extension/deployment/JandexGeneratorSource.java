package org.mendrugo.fibula.extension.deployment;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.logging.Log;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.GeneratorSource;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JandexGeneratorSource implements GeneratorSource
{
    private static final DotName BENCHMARK = DotName.createSimple(Benchmark.class.getName());
    private static final DotName BENCHMARK_MODE = DotName.createSimple(BenchmarkMode.class.getName());
    private static final DotName BLACKHOLE = DotName.createSimple(Blackhole.class.getName());
    private static final DotName SETUP = DotName.createSimple(Setup.class.getName());
    private static final DotName TEAR_DOWN = DotName.createSimple(TearDown.class.getName());
    private static final DotName STATE = DotName.createSimple(State.class.getName());

    private final IndexView index;
    private Collection<ClassInfo> classInfos;

    public JandexGeneratorSource(IndexView index)
    {
        this.index = index;
    }

    @Override
    public Collection<ClassInfo> getClasses()
    {
        if (classInfos != null)
        {
            return classInfos;
        }

        final Map<org.jboss.jandex.ClassInfo, List<AnnotationInstance>> classes = new HashMap<>();
        collectAnnotations(BENCHMARK, classes);
        collectAnnotations(BENCHMARK_MODE, classes);
        collectAnnotations(SETUP, classes);
        collectAnnotations(TEAR_DOWN, classes);

        classInfos = classes.entrySet().stream()
            .map(e -> new JandexClassInfo(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(JandexClassInfo::getQualifiedName))
            .collect(Collectors.toList());
        
        return classInfos;
    }

    private void collectAnnotations(DotName annotation, Map<org.jboss.jandex.ClassInfo, List<AnnotationInstance>> classes)
    {
        final Collection<AnnotationInstance> benchmarkMethods = index.getAnnotations(annotation);
        Log.infof("%d methods found with %s", benchmarkMethods.size(), annotation);
        for (AnnotationInstance benchmarkMethod : benchmarkMethods)
        {
            classes.computeIfAbsent(benchmarkMethod.target().asMethod().declaringClass(), x -> new ArrayList<>())
                .add(benchmarkMethod);
        }
    }

    @Override
    public ClassInfo resolveClass(String className)
    {
        throw new RuntimeException("NYI");
    }
}
