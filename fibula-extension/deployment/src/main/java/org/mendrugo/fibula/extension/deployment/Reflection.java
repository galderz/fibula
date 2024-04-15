package org.mendrugo.fibula.extension.deployment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Reflection
{
    private final Object instance;
    private final Class<?> clazz;

    public Reflection(Object instance, Class<?> clazz)
    {
        this.instance = instance;
        this.clazz = clazz;
    }


    public <T> T invoke(String name, Object arg, Class<?> argClass)
    {
        try
        {
            final Method method = clazz.getDeclaredMethod(name, argClass);
            method.setAccessible(true);
            return Cast.cast(method.invoke(instance, arg));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Reflection error", e);
        }
    }

    public <T> T invoke(String name, Object arg1, Class<?> argClass1, Object arg2, Class<?> argClass2)
    {
        try
        {
            final Method method = clazz.getDeclaredMethod(name, argClass1, argClass2);
            method.setAccessible(true);
            return Cast.cast(method.invoke(instance, arg1, arg2));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Reflection error", e);
        }
    }

    public <T> T field(String name, Object obj)
    {
        try
        {
            final Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return Cast.cast(field.get(obj));
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new RuntimeException("Reflection error", e);
        }
    }
}
