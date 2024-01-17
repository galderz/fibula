package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

final class ProcessRunner
{
    private final OutputFormat out;

    ProcessRunner(OutputFormat out)
    {
        this.out = out;
    }

    Process runFork(int forkIndex, BenchmarkParams params)
    {
        final int forkCount = params.getMeasurement().getCount();
        final List<String> forkArguments = runArguments(params);
        Log.debugf("Executing: %s", String.join(" ", forkArguments));
        out.println("# Fork: " + forkIndex + " of " + forkCount);
        // todo wait for fork to finish before launching the next one
        //      otherwise when trying to use the native image agent you get error that the configuration files are in use
        //      it should also remove some potential noise since we make sure the fork is finished before starting next
        return runAsync(new ProcessBuilder(forkArguments).inheritIO());
    }

    private Process runAsync(ProcessBuilder processBuilder)
    {
        try
        {
            return processBuilder.start();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> runArguments(BenchmarkParams params)
    {
        final List<String> baseArguments = getBaseArguments(params);
        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.PARAMS);
        arguments.add(Serializables.toBase64(params));
        return arguments;
    }

    private static List<String> getBaseArguments(BenchmarkParams params)
    {
        final File jar = new File("target/runner-jvm/quarkus-run.jar");
        final File binary = new File("target/runner-native/fibula-samples-1.0.0-SNAPSHOT-runner");

        final List<String> nativeArguments = List.of(binary.getPath());
        final List<String> jvmArguments = List.of(
            params.getJvm()
            // todo add an option for the native image agent and fix location of java
            // , "-agentlib:native-image-agent=config-output-dir=target/native-agent-config"
            , "-jar"
            , jar.getPath()
        );

        if (jar.exists() && binary.exists())
        {
            if (jar.lastModified() > binary.lastModified())
            {
                return jvmArguments;
            }

            return nativeArguments;
        }

        if (jar.exists())
        {
            return jvmArguments;
        }

        if (binary.exists())
        {
            return nativeArguments;
        }

        throw new IllegalStateException("Neither jar nor binary runner built");
    }
}
