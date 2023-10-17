package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class PackageRunner
{
    void run(PackageTool tool)
    {
        final List<String> arguments = tool.arguments();
        final ProcessBuilder processBuilder = new ProcessBuilder(arguments)
            .inheritIO();
        Log.infof("Executing: %s", String.join(" ", arguments));

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
