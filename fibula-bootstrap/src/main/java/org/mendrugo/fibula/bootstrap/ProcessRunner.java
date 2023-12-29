package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

final class ProcessRunner
{
    private final NativeOptions options;
    private final List<String> buildArguments;

    ProcessRunner(NativeOptions options)
    {
        this.options = options;
        this.buildArguments = buildArguments(options);
    }

    void runBuild()
    {
        Log.debugf("Executing: %s", String.join(" ", buildArguments));
        runSync(new ProcessBuilder(buildArguments).inheritIO());
    }

    void runFork(int forkIndex, int forkCount)
    {
        final List<String> forkArguments = runArguments(options);
        Log.debugf("Executing: %s", String.join(" ", forkArguments));
        // todo use destination instead of System.out
        System.out.printf("# Fork: %d of %d%n", forkIndex, forkCount);
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

    private static List<String> runArguments(NativeOptions options)
    {
        final PackageMode packageMode = options.getPackageMode();
        final List<String> runnerArguments = options.getRunnerArguments();
        // todo avoid hardcoding sample project name
        final List<String> baseArguments = switch (packageMode)
        {
            case JVM -> List.of(
                "java"
                , "-jar"
                , "fibula-samples/target/runner-app/quarkus-run.jar"
            );
            case NATIVE -> List.of(
                "./fibula-samples/target/runner-app/fibula-samples-1.0.0-SNAPSHOT-runner"
            );
        };

        final List<String> arguments = new ArrayList<>(baseArguments);
        arguments.addAll(runnerArguments);
        return arguments;
    }
}
