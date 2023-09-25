package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

//public record BenchmarkInfo(Map<ClassInfo, List<MethodInfo>> info)
public record BenchmarkInfo(MethodInfo info)
{
    public static final class Builder
    {
        // final List<MethodInfo> methods = new ArrayList<>();
        MethodInfo method;

        void withMethod(MethodInfo methodInfo)
        {
            // methods.add(methodInfo);
            this.method = methodInfo;
        }

        BenchmarkInfo build()
        {
//            return new BenchmarkInfo(
//                methods.stream()
//                    .collect(groupingBy(MethodInfo::declaringClass))
//            );
            return new BenchmarkInfo(method);
        }
    }
}
