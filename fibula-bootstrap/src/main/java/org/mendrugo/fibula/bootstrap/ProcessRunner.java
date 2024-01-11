package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

final class ProcessRunner
{
    private final NativeOptions options;
    private final List<String> buildArguments;
    private final OutputFormat out;

    ProcessRunner(NativeOptions options, OutputFormat out)
    {
        this.options = options;
        this.out = out;
        this.buildArguments = buildArguments(options);
    }

    void runBuild()
    {
        Log.debugf("Executing: %s", String.join(" ", buildArguments));
        runSync(new ProcessBuilder(buildArguments).inheritIO());
    }

    void runFork(int forkIndex, BenchmarkParams params)
    {
        final int forkCount = params.getMeasurement().getCount();
        final List<String> forkArguments = runArguments(options, params);
        Log.debugf("Executing: %s", String.join(" ", forkArguments));
        out.println("# Fork: " + forkIndex + " of " + forkCount);
        // todo wait for fork to finish before launching the next one
        //      otherwise when trying to use the native image agent you get error that the configuration files are in use
        //      it should also remove some potential noise since we make sure the fork is finished before starting next
        runAsync(new ProcessBuilder(forkArguments).inheritIO());
    }

    private void runAsync(ProcessBuilder processBuilder)
    {
        try
        {
            processBuilder.start();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void runSync(ProcessBuilder processBuilder)
    {
        try
        {
            final Process process = processBuilder.start();
            final int exitCode = process.waitFor();
            if (exitCode != 0)
            {
                throw new RuntimeException(String.format(
                    "Error packaging runner (exit code %d)"
                    , exitCode
                ));
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> buildArguments(NativeOptions options)
    {
        final PackageMode packageMode = options.getPackageMode();
        // todo avoid hardcoding sample project name
        final List<String> baseArguments = switch (packageMode)
        {
            case JVM -> List.of(
                "mvn"
                , "package"
                , "-DskipTests"
                , "-pl"
                , "fibula-samples"
                , "-Prunner"
            );
            case NATIVE -> List.of(
                "mvn"
                , "package"
                , "-DskipTests"
                , "-pl"
                , "fibula-samples"
                , "-Prunner-native"
            );
        };

        final List<String> arguments = new ArrayList<>(baseArguments);
        if (options.isDecompile())
        {
            arguments.add("-Dquarkus.package.vineflower.enabled=true");
        }

        return arguments;
    }

    private static List<String> runArguments(NativeOptions options, BenchmarkParams params)
    {
        final PackageMode packageMode = options.getPackageMode();
        // todo avoid hardcoding sample project name
        final List<String> baseArguments = switch (packageMode)
        {
            case JVM -> List.of(
                // todo add an option for the native image agent and fix location of java
                // ".../graal-21/bin/java"
                // , "-agentlib:native-image-agent=config-output-dir=target/native-agent-config"
                "java"
                , "-jar"
                , "fibula-samples/target/runner-app/quarkus-run.jar"
            );
            case NATIVE -> List.of(
                "./fibula-samples/target/runner-app/fibula-samples-1.0.0-SNAPSHOT-runner"
            );
        };

        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.add("--" + RunnerArguments.PARAMS);
        arguments.add(Serializables.toBase64(params));
        return arguments;
    }
}
