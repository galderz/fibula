package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;

import java.lang.annotation.Annotation;
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
        return new JandexClassInfo(jandexField.type().name(), index);
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
