package org.mendrugo.fibula.it.profilers;

import org.junit.Test;
import org.mendrugo.fibula.bootstrap.BenchmarkService;
import org.openjdk.jmh.it.Fixtures;
import org.openjdk.jmh.it.profilers.ItExternalProfiler;
import org.openjdk.jmh.it.profilers.ItInternalProfiler;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class ProfilerTest extends org.openjdk.jmh.it.profilers.ProfilerTest
{
    @Override
    @Test
    public void testInternal_API() throws RunnerException
    {
        Options opts = new OptionsBuilder()
            .include(Fixtures.getTestMask(this.getClass().getSuperclass()))
            .addProfiler(ItInternalProfiler.class)
            .build();
        new BenchmarkService().run(opts);
    }

    @Override
    @Test
    public void testExternal_API() throws RunnerException
    {
        Options opts = new OptionsBuilder()
            .include(Fixtures.getTestMask(this.getClass().getSuperclass()))
            .addProfiler(ItExternalProfiler.class)
            .build();
        new BenchmarkService().run(opts);
    }

    @Override
    @Test
    public void testInternal_CLI() throws RunnerException, CommandLineOptionException
    {
        Options opts = new CommandLineOptions("-prof", ItInternalProfiler.class.getCanonicalName(), Fixtures.getTestMask(this.getClass().getSuperclass()));
        new BenchmarkService().run(opts);
    }

    @Override
    @Test
    public void testExternal_CLI() throws RunnerException, CommandLineOptionException
    {
        Options opts = new CommandLineOptions("-prof", ItExternalProfiler.class.getCanonicalName(), Fixtures.getTestMask(this.getClass().getSuperclass()));
        new BenchmarkService().run(opts);
    }

    @Test
    public void testNonNullMetadata() throws RunnerException, CommandLineOptionException
    {
        Options opts = new CommandLineOptions("-prof", ValidateExternalProfiler.class.getCanonicalName(), Fixtures.getTestMask(this.getClass().getSuperclass()));
        new BenchmarkService().run(opts);
    }
}
