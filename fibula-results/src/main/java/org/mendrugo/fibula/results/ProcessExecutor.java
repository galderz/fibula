package org.mendrugo.fibula.results;

import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.TempFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

public final class ProcessExecutor
{
    private final OutputFormat out;

    public ProcessExecutor(OutputFormat out)
    {
        this.out = out;
    }

    public ProcessResult runSync(List<String> arguments, boolean printOut, boolean printErr)
    {
        // todo add profiler and option overrides for print*

        TempFile stdOut;
        TempFile stdErr;
        try
        {
            stdOut = FileUtils.weakTempFile("stdout");
            stdErr = FileUtils.weakTempFile("stderr");
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        try (FileOutputStream fosErr = new FileOutputStream(stdErr.file())
             ; FileOutputStream fosOut = new FileOutputStream(stdOut.file())
        )
        {
            final ProcessBuilder processBuilder = new ProcessBuilder(arguments);
            long startTime = System.currentTimeMillis();
            final Process process = processBuilder.start();

            // drain streams, else we might lock up
            final InputStreamDrainer errDrainer = new InputStreamDrainer(process.getErrorStream(), fosErr);
            final InputStreamDrainer outDrainer = new InputStreamDrainer(process.getInputStream(), fosOut);

            if (printErr)
            {
                errDrainer.addOutputStream(new OutputFormatAdapter(out));
            }

            if (printOut)
            {
                outDrainer.addOutputStream(new OutputFormatAdapter(out));
            }

            errDrainer.start();
            outDrainer.start();

            int exitValue = process.waitFor();

            errDrainer.join();
            outDrainer.join();

            return new ProcessResult(exitValue, stdOut, stdErr, startTime);
        }
        catch (IOException e)
        {
            out.println("<failed to invoke the VM, caught IOException: " + e.getMessage() + ">");
            out.println("");
            throw new UncheckedIOException(e);
        }
        catch (InterruptedException e)
        {
            out.println("<host VM has been interrupted waiting for forked VM: " + e.getMessage() + ">");
            out.println("");
            throw new BenchmarkException(e);
        }
    }

    private static final class OutputFormatAdapter extends OutputStream
    {
        private final OutputFormat out;

        public OutputFormatAdapter(OutputFormat out)
        {
            this.out = out;
        }

        @Override
        public void write(int b)
        {
            out.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException
        {
            out.write(b);
        }
    }

    public record ProcessResult(
        int exitCode
        , TempFile stdOut
        , TempFile stdErr
        , long startTime
    ) {}
}
