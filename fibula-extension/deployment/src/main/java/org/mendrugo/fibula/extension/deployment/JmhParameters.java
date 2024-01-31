package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Optional;
import org.openjdk.jmh.util.lines.TestLineWriter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class JmhParameters
{
    static String asLine(MethodInfo methodInfo, Mode mode, AnnotationInstance outputTimeUnitAnnotation)
    {
        final ClassInfo classInfo = methodInfo.declaringClass();

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
        final Optional<TimeUnit> tu = outputTimeUnitAnnotation != null
            ? Optional.of(TimeUnit.valueOf(outputTimeUnitAnnotation.value().asEnum()))
            : Optional.none();
        final Optional<Integer> opsPerInvocation = Optional.none();
        final Optional<TimeValue> timeout = Optional.none();

        // JMH
        TestLineWriter writer = new TestLineWriter();
        writer.putString(classInfo.name().toString());
        writer.putString(classInfo.simpleName());
        writer.putString(methodInfo.name());
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

    private JmhParameters() {}
}