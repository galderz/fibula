package org.mendrugo.fibula.results;

import java.util.Optional;

public class JmhOptionals
{
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> Optional<T> fromJmh(org.openjdk.jmh.util.Optional jmhOptional)
    {
        return jmhOptional.hasValue()
            ? (Optional<T>) Optional.of(jmhOptional.get())
            : Optional.empty();
    }
}
