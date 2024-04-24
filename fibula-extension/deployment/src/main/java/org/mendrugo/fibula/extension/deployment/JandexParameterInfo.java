package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MethodInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.stream.Collectors;

public record JandexParameterInfo(
    MethodParameterInfo jandexParam
    , IndexView index
) implements ParameterInfo
{
    @Override
    public ClassInfo getType()
    {
        // todo find a common impl that works for field class info, param class info and standard class info
        return new ClassInfo()
        {
            @Override
            public String getPackageName()
            {
                return jandexParam.type().name().packagePrefix();
            }

            @Override
            public String getQualifiedName()
            {
                return jandexParam.type().name().toString();
            }

            @Override
            public String getName()
            {
                return jandexParam.type().name().withoutPackagePrefix();
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
                return index.getClassByName(jandexParam.type().name()).fields().stream()
                    .map(f -> new JandexFieldInfo(f, index))
                    .collect(Collectors.toList());
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
                    .filter(annotation -> annotation.target().asClass().name().equals(jandexParam.type().name()))
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
}
