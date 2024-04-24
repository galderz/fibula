package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Optional;

public record JandexFieldInfo(
    org.jboss.jandex.FieldInfo jandexField
    , IndexView index
) implements FieldInfo
{
    @Override
    public String getName()
    {
        return jandexField.name();
    }

    @Override
    public ClassInfo getType()
    {
        // todo find a common impl that works for field class info, param class info and standard class info
        return new ClassInfo()
        {
            @Override
            public String getPackageName()
            {
                return jandexField.type().name().packagePrefix();
            }

            @Override
            public String getQualifiedName()
            {
                return jandexField.type().name().toString();
            }

            @Override
            public String getName()
            {
                return null;  // TODO: Customise this generated block
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
                return null;  // TODO: Customise this generated block
            }

            @Override
            public Collection<MethodInfo> getMethods()
            {
                return null;  // TODO: Customise this generated block
            }

            @Override
            public Collection<MethodInfo> getConstructors()
            {
                return null;  // TODO: Customise this generated block
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annClass)
            {
                final DotName name = DotName.createSimple(annClass);
                return index.getAnnotations(name).stream()
                    .filter(annotation -> annotation.target().asClass().name().equals(jandexField.type().name()))
                    .findFirst()
                    .map(ann -> JandexAnnotations.instantiate(ann, annClass))
                    .orElse(null);
            }

            @Override
            public boolean isAbstract()
            {
                return false;  // TODO: Customise this generated block
            }

            @Override
            public boolean isPublic()
            {
                return false;  // TODO: Customise this generated block
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
        };
    }

    @Override
    public ClassInfo getDeclaringClass()
    {
        return null;  // TODO: Customise this generated block
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annClass)
    {
        final DotName name = DotName.createSimple(annClass);
        return Optional.ofNullable(jandexField.annotation(name))
            .map(ann -> JandexAnnotations.instantiate(ann, annClass))
            .orElse(null);
    }

    @Override
    public boolean isPublic()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean isStatic()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean isFinal()
    {
        return false;  // TODO: Customise this generated block
    }
}
