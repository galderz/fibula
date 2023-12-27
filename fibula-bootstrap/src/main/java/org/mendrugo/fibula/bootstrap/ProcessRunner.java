package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

final class ProcessRunner
{
    private final NativeOptions options;
    private final List<String> forkArguments;
    private final List<String> buildArguments;

    ProcessRunner(NativeOptions options, PackageTool tool)
    {
        this.options = options;
        this.buildArguments = tool.buildArguments(options.getPackageMode());
        this.forkArguments = tool.runArguments(options.getPackageMode(), options.getRunnerArguments());
    }

    void runBuild()
    {
        Log.infof("Executing: %s", String.join(" ", buildArguments));
        runSync(new ProcessBuilder(buildArguments).inheritIO());
    }

    void runFork(int forkCount)
    {
        Log.infof("Executing: %s", String.join(" ", forkArguments));
        System.out.printf("# Fork: %d of %d", forkCount + 1, options.getMeasurementForks());
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
}
