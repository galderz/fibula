package org.mendrugo.fibula.it.profilers;

import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class ProfilerTest
{
    @Test
    public void testInternal_API() throws RunnerException
    {
        Options opts = new OptionsBuilder()
            .include(ProfilerIB.class.getSimpleName())
            .shouldFailOnError(true)
            .addProfiler(ItInternalProfiler.class)
            .build();
        new BenchmarkService().run(opts);
    }

    @Test
    public void testExternal_API() throws RunnerException
    {
        Options opts = new OptionsBuilder()
            .include(ProfilerIB.class.getCanonicalName())
            .shouldFailOnError(true)
            .addProfiler(ItExternalProfiler.class)
            .build();
        new BenchmarkService().run(opts);
    }

    @Test
    public void testInternal_CLI() throws RunnerException, CommandLineOptionException
    {
        Options opts = new CommandLineOptions(
            "-prof"
            , ItInternalProfiler.class.getCanonicalName()
            , "-foe"
            , "true"
            , ProfilerIB.class.getCanonicalName()
        );
        new BenchmarkService().run(opts);
    }

    @Test
    public void testExternal_CLI() throws RunnerException, CommandLineOptionException
    {
        Options opts = new CommandLineOptions(
            "-prof"
            , ItExternalProfiler.class.getCanonicalName()
            , "-foe"
            , "true"
            , ProfilerIB.class.getCanonicalName()
        );
        new BenchmarkService().run(opts);
    }

    @Test
    public void testNonNullMetadata() throws RunnerException, CommandLineOptionException
    {
        Options opts = new CommandLineOptions(
            "-prof"
            , ValidateExternalProfiler.class.getCanonicalName()
            , "-foe"
            , "true"
            , ProfilerIB.class.getCanonicalName()
        );
        new BenchmarkService().run(opts);
    }
}
