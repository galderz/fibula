package org.mendrugo.fibula.bootstrap;

import io.quarkus.runtime.annotations.CommandLineArguments;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;

@ApplicationScoped
public class JmhOptionsProducer
{
    @Produces
    public Options jmhOptions(@CommandLineArguments String[] args)
    {
        try
        {
            return new CommandLineOptions(args);
        }
        catch (CommandLineOptionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
