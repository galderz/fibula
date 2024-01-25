package org.mendrugo.fibula.extension.deployment;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.generators.core.GenerationException;

import java.util.HashMap;
import java.util.Map;

// Inspired by JMH. Equivalent class is package private there.
public class Identifiers
{
    private final Map<String, String> collapsedTypes = new HashMap<>();
    private int collapsedIndex = 0;
    private int index = 0;

    public String collapseTypeName(String e)
    {
        if (collapsedTypes.containsKey(e))
        {
            return collapsedTypes.get(e);
        }

        String[] strings = e.split("\\.");
        String name = strings[strings.length - 1].toLowerCase();

        String collapsedName = name + (collapsedIndex++) + "_";
        collapsedTypes.put(e, collapsedName);
        return collapsedName;
    }

    public String identifier(Scope scope)
    {
        switch (scope)
        {
            case Benchmark:
            case Group:
            {
                return "G";
            }
            case Thread:
            {
                return String.valueOf(index++);
            }
            default:
                throw new GenerationException("Unknown scope: " + scope, null);
        }
    }
}
