package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class BenchmarkInfoBuilder
{
    final List<MethodInfo> methods = new ArrayList<>();

    void withMethod(MethodInfo methodInfo)
    {
        methods.add(methodInfo);
    }

    Map<ClassInfo, List<MethodInfo>> build()
    {
        return methods.stream()
            .collect(groupingBy(MethodInfo::declaringClass));
    }
}
