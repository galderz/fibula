package org.mendrugo.fibula.results;

public final class Unchecked
{
    @SuppressWarnings("unchecked")
    static <T> T cast(Object obj)
    {
        return (T) obj;
    }

    private Unchecked()
    {
    }
}
