package org.mendrugo.fibula.extension.deployment;

import io.quarkus.logging.Log;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JandexGeneratorSource implements GeneratorSource
{
    private static final DotName BENCHMARK = DotName.createSimple(Benchmark.class.getName());
    private static final DotName BENCHMARK_MODE = DotName.createSimple(BenchmarkMode.class.getName());
    private static final DotName BLACKHOLE = DotName.createSimple(Blackhole.class.getName());
    private static final DotName STATE = DotName.createSimple(State.class.getName());
    private static final DotName SETUP = DotName.createSimple(Setup.class.getName());
    private static final DotName TEAR_DOWN = DotName.createSimple(TearDown.class.getName());

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
        collectAnnotatedTypes(BENCHMARK, classes);
        collectAnnotatedTypes(STATE, classes);
        collectAnnotatedMethods(BENCHMARK, classes);
        collectAnnotatedMethods(BENCHMARK_MODE, classes);
        collectAnnotatedMethods(SETUP, classes);
        collectAnnotatedMethods(TEAR_DOWN, classes);

        classInfos = classes.entrySet().stream()
            .map(e -> new JandexClassInfo(e.getKey(), e.getValue(), index))
            .filter(generateFilter())
            .sorted(Comparator.comparing(JandexClassInfo::getQualifiedName))
            .collect(Collectors.toList());
        
        return classInfos;
    }

    private static Predicate<ClassInfo> generateFilter()
    {
        final String generate = System.getProperty("fibula.generate");
        if (generate != null && !generate.isEmpty())
        {
            Log.info("Generate only: " + generate);
            return classInfo -> classInfo.getQualifiedName().contains(generate);
        }

        return x -> true;
    }

    private void collectAnnotatedTypes(DotName annotation, Map<org.jboss.jandex.ClassInfo, List<AnnotationInstance>> classes)
    {
        final Collection<AnnotationInstance> annotations = index.getAnnotations(annotation).stream()
            .filter(ann -> ann.target() instanceof org.jboss.jandex.ClassInfo)
            .toList();
        Log.infof("%d classes found with %s", annotations.size(), annotation);
        annotations.forEach(ann -> classes.computeIfAbsent(ann.target().asClass(), x -> new ArrayList<>()));
    }

    private void collectAnnotatedMethods(DotName annotation, Map<org.jboss.jandex.ClassInfo, List<AnnotationInstance>> classes)
    {
        final Collection<AnnotationInstance> annotatedMethods = index.getAnnotations(annotation).stream()
            .filter(ann -> ann.target() instanceof MethodInfo)
            .toList();
        Log.infof("%d methods found with %s", annotatedMethods.size(), annotation);
        annotatedMethods.forEach(ann -> classes
            .computeIfAbsent(ann.target().asMethod().declaringClass(), x -> new ArrayList<>())
            .add(ann)
        );
    }

    @Override
    public ClassInfo resolveClass(String className)
    {
        throw new RuntimeException("NYI");
    }
}
