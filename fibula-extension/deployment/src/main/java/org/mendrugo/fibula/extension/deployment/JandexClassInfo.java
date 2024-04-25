package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public record JandexClassInfo(
    DotName name
    , IndexView index
) implements ClassInfo
{
    @Override
    public String getPackageName()
    {
        return name.packagePrefix();
    }

    @Override
    public String getQualifiedName()
    {
        return name.toString();
    }

    @Override
    public String getName()
    {
        return name.withoutPackagePrefix();
    }

    @Override
    public ClassInfo getSuperClass()
    {
        // todo support super classes
        return null;
    }

    @Override
    public ClassInfo getDeclaringClass()
    {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public Collection<FieldInfo> getFields()
    {
        final org.jboss.jandex.ClassInfo jandexClass = index.getClassByName(name);
        if (jandexClass == null)
        {
            return Collections.emptyList();
        }

        return jandexClass.fields().stream()
            .map(f -> new JandexFieldInfo(f, index))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<MethodInfo> getMethods()
    {
        final org.jboss.jandex.ClassInfo jandexClass = index.getClassByName(name);
        if (jandexClass == null)
        {
            return Collections.emptyList();
        }

        return jandexClass.methods().stream()
            .map(m -> new JandexMethodInfo(m, index))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<MethodInfo> getConstructors()
    {
        return index.getClassByName(name).constructors().stream()
            .map(ctor -> new JandexMethodInfo(ctor, index))
            .collect(Collectors.toList());
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass)
    {
        final DotName annDotName = DotName.createSimple(annClass);
        final AnnotationTarget jandexClass = index.getClassByName(name);
        if (jandexClass == null)
        {
            return null;
        }

        return jandexClass.declaredAnnotations().stream()
            .filter(annotation -> annotation.name().equals(annDotName))
            .findFirst()
            .map(ann -> JandexAnnotations.instantiate(ann, annClass))
            .orElse(null);
    }

    @Override
    public boolean isAbstract()
    {
        return Modifier.isAbstract(index.getClassByName(name).flags());
    }

    @Override
    public boolean isPublic()
    {
        return Modifier.isPublic(index.getClassByName(name).flags());
    }

    @Override
    public boolean isStrictFP()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean isFinal()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean isInner()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean isEnum()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public Collection<String> getEnumConstants()
    {
        return null;  // TODO: Customise this generated block
    }
}
