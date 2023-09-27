package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.MethodInfo;

import java.util.ArrayList;
import java.util.List;

public record BenchmarkInfo(List<MethodInfo> methods)
{
    public static final class Builder
    {
        final List<MethodInfo> methods = new ArrayList<>();

        void withMethod(MethodInfo methodInfo)
        {
            methods.add(methodInfo);
        }

        BenchmarkInfo build()
        {
            return new BenchmarkInfo(methods);
        }
    }
}