package org.mendrugo.fibula;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CapturingRunner
{
    static Collection<RunResult> run(Consumer<ChainedOptionsBuilder> block) throws RunnerException
    {
        return run(block, ignored -> {});
    }

    static Collection<RunResult> run(Consumer<ChainedOptionsBuilder> block, Consumer<Collection<String>> expect) throws RunnerException
    {
        File output = null;
        try
        {
            output = FileUtils.tempFile("output");

            final ChainedOptionsBuilder builder = new OptionsBuilder()
                .output(output.getAbsolutePath());

            // Clear up jvm arguments for the forked process when debugging runner,
            // otherwise forked process also starts with debugging parameters
            if ("runner".equals(System.getenv("DEBUG")))
            {
                builder.jvmArgs();
            } else if ("fork".equals(System.getenv("DEBUG")))
            {
                builder.jvmArgsPrepend("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:6006");
            }

            block.accept(builder);

            final Options opts = builder.build();
            return new MultiVmRunner(opts).run();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        finally
        {
            if (output != null)
            {
                try
                {
                    final Collection<String> lines = FileUtils.readAllLines(output);
                    lines.forEach(System.err::println);

                    if (ForkedVm.instance().isNativeVm())
                    {
                        assertTrue(lines.stream().anyMatch(line -> line.contains("Substrate VM")));
                    }
                    else
                    {
                        assertTrue(lines.stream().anyMatch(line -> line.contains("OpenJDK")));
                    }
                    expect.accept(lines);
                }
                catch (IOException e)
                {
                    // Ignore
                }
            }
        }
    }
}
