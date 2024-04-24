package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

// todo find a common impl that works for field class info, param class info and standard class info
public record JandexClassInfo(
    org.jboss.jandex.ClassInfo jandexClass
    , List<AnnotationInstance> annotatedMethods
    , IndexView index
) implements ClassInfo
{
    @Override
    public String getPackageName()
    {
        return jandexClass.name().packagePrefix();
    }

    @Override
    public String getQualifiedName()
    {
        return jandexClass.name().toString();
    }

    @Override
    public String getName()
    {
        return jandexClass.simpleName();
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
        return jandexClass.fields().stream()
            .map(f -> new JandexFieldInfo(f, index))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<MethodInfo> getMethods()
    {
        final Map<org.jboss.jandex.MethodInfo, List<AnnotationInstance>> annotationsPerMethod =
            annotatedMethods.stream()
                .collect(groupingBy(ann -> ann.target().asMethod()));

        return annotationsPerMethod.entrySet().stream()
            .map(e -> new JandexMethodInfo(e.getKey(), e.getValue(), this, index))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<MethodInfo> getConstructors()
    {
        return jandexClass.constructors().stream()
            .map(ctor -> new JandexMethodInfo(ctor, Collections.emptyList(), this, index))
            .collect(Collectors.toList());
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass)
    {
        final DotName name = DotName.createSimple(annClass);
        return jandexClass.declaredAnnotations().stream()
            .filter(annotation -> annotation.name().equals(name))
            .findFirst()
            .map(ann -> JandexAnnotations.instantiate(ann, annClass))
            .orElse(null);
    }

    @Override
    public boolean isAbstract()
    {
        return Modifier.isAbstract(jandexClass.flags());
    }

    @Override
    public boolean isPublic()
    {
        return Modifier.isPublic(jandexClass.flags());
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
