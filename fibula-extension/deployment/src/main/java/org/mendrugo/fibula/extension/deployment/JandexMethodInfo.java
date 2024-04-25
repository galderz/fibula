package org.mendrugo.fibula.extension.deployment;

import io.quarkus.gizmo.MethodDescriptor;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.MethodInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.stream.Collectors;

public record JandexMethodInfo(
    org.jboss.jandex.MethodInfo jandexMethod
    , IndexView index
) implements MethodInfo
{
    public MethodDescriptor getMethodDescriptor()
    {
        return MethodDescriptor.of(jandexMethod);
    }

    public Type getJandexReturnType()
    {
        return jandexMethod.returnType();
    }

    @Override
    public String getName()
    {
        return jandexMethod.name();
    }

    @Override
    public String getQualifiedName()
    {
        return String.format("%s.%s"
            , jandexMethod.declaringClass().name().toString()
            , jandexMethod
        );
    }

    @Override
    public String getReturnType()
    {
        return jandexMethod.returnType().name().toString();
    }

    @Override
    public Collection<ParameterInfo> getParameters()
    {
        return jandexMethod.parameters().stream()
            .map(m -> new JandexParameterInfo(m, index))
            .collect(Collectors.toList());
    }

    @Override
    public ClassInfo getDeclaringClass()
    {
        return new JandexClassInfo(jandexMethod.declaringClass().name(), index);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass)
    {
        final DotName name = DotName.createSimple(annClass);
        return jandexMethod.annotations().stream()
            .filter(annotation -> annotation.name().equals(name))
            .findFirst()
            .map(ann -> JandexAnnotations.instantiate(ann, annClass))
            .orElse(null);
    }

    @Override
    public boolean isPublic()
    {
        return Modifier.isPublic(jandexMethod.flags());
    }

    @Override
    public boolean isAbstract()
    {
        return Modifier.isAbstract(jandexMethod.flags());
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
