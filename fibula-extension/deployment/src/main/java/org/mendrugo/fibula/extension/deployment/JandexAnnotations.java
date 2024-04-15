package org.mendrugo.fibula.extension.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

final class JandexAnnotations
{
    static <T extends Annotation> T instantiate(AnnotationInstance ann, Class<T> annClass)
    {
        return Cast.cast(Proxy.newProxyInstance(
            annClass.getClassLoader()
            , new Class[]{annClass}
            , invocationHandler(ann)
        ));
    }

    private static InvocationHandler invocationHandler(AnnotationInstance ann)
    {
        final List<AnnotationValue> values = ann.values();
        if (values.isEmpty())
        {
            return (proxy, method, args) -> method.getDefaultValue();
        }

        final Map<String, AnnotationValue> valueMap = values.stream()
            .collect(toMap(
                AnnotationValue::name
                , annValue -> annValue
                , (existing, replacement) -> existing)
            );

        return (proxy, method, args) ->
        {
            String methodName = method.getName();
            final AnnotationValue annValue = valueMap.get(methodName);
            if (annValue != null)
            {
                return switch (annValue.kind())
                {
                    case AnnotationValue.Kind.ARRAY -> toEnumArray(annValue, method);
                    case AnnotationValue.Kind.ENUM -> toEnum(annValue, method);
                    default -> annValue.value();
                };
            }
            return method.getDefaultValue();
        };
    }

    private static Object toEnum(AnnotationValue enumValue, Method method)
    {
        try
        {
            final Class<?> enumClass = method.getReturnType();
            final Method valueOf = enumClass.getMethod("valueOf", String.class);
            return valueOf.invoke(null, enumValue.asEnum());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error constructing enum", e);
        }
    }

    private static Object toEnumArray(AnnotationValue enumValue, Method method)
    {
        try
        {
            final Class<?> enumClass = method.getReturnType().getComponentType();
            final List<AnnotationValue> enumList = enumValue.asArrayList();
            final Object result = Array.newInstance(enumClass, enumList.size());
            for (int i = 0; i < enumList.size(); i++)
            {
                final Method valueOf = enumClass.getMethod("valueOf", String.class);
                Array.set(result, i, valueOf.invoke(null, enumList.get(i).asEnum()));
            }
            return result;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error constructing enum array", e);
        }
    }

    private JandexAnnotations()
    {
    }
}
