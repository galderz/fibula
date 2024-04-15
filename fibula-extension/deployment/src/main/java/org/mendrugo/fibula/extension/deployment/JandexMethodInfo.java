package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.MethodInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;

public record JandexMethodInfo(
    org.jboss.jandex.MethodInfo jandex
    , Collection<AnnotationInstance> annotations
    , ClassInfo classInfo
) implements MethodInfo
{
    @Override
    public String getName()
    {
        return jandex.name();
    }

    @Override
    public String getQualifiedName()
    {
        return classInfo.getQualifiedName() + "." + jandex.toString();
    }

    @Override
    public String getReturnType()
    {
        return jandex.returnType().name().toString();
    }

    @Override
    public Collection<ParameterInfo> getParameters()
    {
        // todo support @Param annotation
        return Collections.emptyList();
    }

    @Override
    public ClassInfo getDeclaringClass()
    {
        return classInfo;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass)
    {
        final DotName name = DotName.createSimple(annClass);
        return annotations.stream()
            .filter(annotation -> annotation.name().equals(name))
            .findFirst()
            .map(ann -> JandexAnnotations.instantiate(ann, annClass))
            .orElse(null);
    }

    @Override
    public boolean isPublic()
    {
        return Modifier.isPublic(jandex.flags());
    }

    @Override
    public boolean isAbstract()
    {
        return Modifier.isAbstract(jandex.flags());
    }

    @Override
    public boolean isSynchronized()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean isStrictFP()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean isStatic()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public int compareTo(MethodInfo o)
    {
        return getQualifiedName().compareTo(o.getQualifiedName());
    }
}
