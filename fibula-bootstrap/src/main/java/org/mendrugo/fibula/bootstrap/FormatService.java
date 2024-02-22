package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.runner.format.OutputFormat;

@ApplicationScoped
public class FormatService
{
    private final OutputFormat out;

    public FormatService()
    {
        this.out = JmhFormats.outputFormat();
    }

    OutputFormat output()
    {
        return this.out;
    }
}
