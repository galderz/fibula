package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;

public record JandexParameterInfo(
    MethodParameterInfo jandexParam
    , IndexView index
) implements ParameterInfo
{
    @Override
    public ClassInfo getType()
    {
        return new JandexClassInfo(jandexParam.type().name(), index);
    }
}
