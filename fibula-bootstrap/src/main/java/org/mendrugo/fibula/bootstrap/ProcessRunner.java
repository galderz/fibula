package org.mendrugo.fibula.bootstrap;

import java.io.IOException;
import java.io.UncheckedIOException;

public class ProcessRunner
{
    void runAsync(ProcessBuilder processBuilder)
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

    void runSync(ProcessBuilder processBuilder)
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
