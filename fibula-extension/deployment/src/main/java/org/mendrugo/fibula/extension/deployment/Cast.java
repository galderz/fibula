package org.mendrugo.fibula.extension.deployment;

public final class Cast
{
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj)
    {
        return (T) obj;
    }

    private Cast() {}
}
