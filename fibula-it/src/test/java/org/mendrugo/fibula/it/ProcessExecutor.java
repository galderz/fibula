package org.mendrugo.fibula.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class ProcessExecutor
{
    static void runSync(List<String> arguments, Parameters parameters, Provider provider)
    {
        System.out.println("Executing: " + String.join(" ", arguments));
        final ProcessBuilder processBuilder = new ProcessBuilder().command(arguments);
        processBuilder.directory(provider.workingDir().toFile());
        try
        {
            Process process = processBuilder.start();
            StreamGobbler pOut = new StreamGobbler(process.getInputStream(), new PrintStream(System.out));
            StreamGobbler pErr = new StreamGobbler(process.getErrorStream(), new PrintStream(System.out));
            pOut.start();
            pErr.start();

            final boolean success = process.waitFor(parameters.timeoutMins(), TimeUnit.MINUTES);
            if (!success)
            {
                throw new AssertionError("Benchmark did not finish in allocated time");
            }
        }
        catch (IOException | InterruptedException e)
        {
            throw new AssertionError(e);
        }
    }

    private static class StreamGobbler extends Thread
    {
        private final InputStream in;
        private final PrintStream out;

        private StreamGobbler(InputStream in, PrintStream out)
        {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run()
        {
            try
            {
                final BufferedReader input = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = input.readLine()) != null)
                {
                    out.println(line);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
