package org.mendrugo.fibula.generator;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.GeneratorSource;
import org.openjdk.jmh.generators.core.MethodInfo;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class BenchmarkGeneratorUtils
{
    public static <T extends Annotation> Collection<MethodInfo> getMethodsAnnotatedWith(
        GeneratorSource source
        , Class<T> annClass
    )
    {
        final List<MethodInfo> result = new ArrayList<>();
        for (ClassInfo ci : source.getClasses())
        {
            for (MethodInfo mi : ci.getMethods())
            {
                if (mi.getAnnotation(annClass) != null)
                {
                    result.add(mi);
                }
            }
        }
        return result;
    }
}
