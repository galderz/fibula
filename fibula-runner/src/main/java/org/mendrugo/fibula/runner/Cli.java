package org.mendrugo.fibula.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

final class Cli
{
    private final Map<String, List<String>> params;

    Cli(Map<String, List<String>> params) {
        this.params = params;
    }

    List<String> multi(String name)
    {
        final var options = params.get(name);
        return options == null ? List.of() : options;
    }

    String required(String name)
    {
        final var option = params.get(name);
        if (null == option)
            throw new IllegalArgumentException(String.format(
                "Missing mandatory --%s"
                , name
            ));

        return option.getFirst();
    }

    String optional(String name, String defaultValue)
    {
        final var option = params.get(name);
        return option != null ? option.getFirst() : defaultValue;
    }

    static Cli read(String... args)
    {
        final Map<String, List<String>> params = new HashMap<>();

        List<String> options = null;
        for (final String arg : args)
        {
            if (arg.startsWith("--"))
            {
                if (arg.length() < 3)
                {
                    throw new IllegalArgumentException(
                        String.format("Error at argument %s", arg)
                    );
                }

                options = new ArrayList<>();
                params.put(arg.substring(2), options);
            }
            else if (options != null)
            {
                options.add(arg);
            }
            else
            {
                throw new IllegalArgumentException("Illegal parameter usage");
            }
        }

        return new Cli(params);
    }
}
