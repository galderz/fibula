package org.mendrugo.fibula;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import org.openjdk.jmh.util.Utils;

import java.util.List;
import java.util.Map;

class ProfilerOptionFormatter implements HelpFormatter
{
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final String name;

    public ProfilerOptionFormatter(String name)
    {
        this.name = name;
    }

    public String format(Map<String, ? extends OptionDescriptor> options)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: -prof <profiler-name>:opt1=value1,value2;opt2=value3");
        sb.append(LINE_SEPARATOR);
        sb.append(LINE_SEPARATOR);
        sb.append("Options accepted by ").append(name).append(":");
        for (OptionDescriptor each : options.values())
        {
            sb.append(lineFor(each));
        }

        return sb.toString();
    }

    private String lineFor(OptionDescriptor d)
    {
        StringBuilder line = new StringBuilder();

        StringBuilder o = new StringBuilder();
        o.append("  ");
        for (String str : d.options())
        {
            if (d.representsNonOptions()) continue;
            o.append(str);
            if (d.acceptsArguments())
            {
                o.append("=");
                if (d.requiresArgument())
                {
                    o.append("<");
                } else
                {
                    o.append("[");
                }
                o.append(d.argumentDescription());
                if (d.requiresArgument())
                {
                    o.append(">");
                } else
                {
                    o.append("]");
                }
            }
        }

        final int optWidth = 35;

        line.append(String.format("%-" + optWidth + "s", o.toString()));
        boolean first = true;
        String desc = d.description();
        List<?> defaults = d.defaultValues();
        if (defaults != null && !defaults.isEmpty())
        {
            desc += " (default: " + defaults.toString() + ")";
        }
        for (String l : Utils.rewrap(desc))
        {
            if (first)
            {
                first = false;
            } else
            {
                line.append(LINE_SEPARATOR);
                line.append(String.format("%-" + optWidth + "s", ""));
            }
            line.append(l);
        }

        line.append(LINE_SEPARATOR);
        line.append(LINE_SEPARATOR);
        return line.toString();
    }

}
