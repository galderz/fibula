package org.mendrugo.fibula;

import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.TempFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

final class Execute
{
    private static final int TAIL_LINES_ON_ERROR = Integer.getInteger("jmh.tailLines", 20);

    private final boolean print;
    private final OutputFormat out;

    Execute(boolean print, OutputFormat out) {
        this.print = print;
        this.out = out;
    }

    void execute(List<String> commandString)
    {
        try
        {
            final TempFile stdErr = FileUtils.weakTempFile("stderr");
            final TempFile stdOut = FileUtils.weakTempFile("stdout");

            try (FileOutputStream fosErr = new FileOutputStream(stdErr.file())
                 ; FileOutputStream fosOut = new FileOutputStream(stdOut.file())
            )
            {
                final ProcessBuilder pb = new ProcessBuilder(commandString);
                final Process p = pb.start();

                // drain streams, else we might lock up
                final InputStreamDrainer errDrainer = new InputStreamDrainer(p.getErrorStream(), fosErr);
                final InputStreamDrainer outDrainer = new InputStreamDrainer(p.getInputStream(), fosOut);

                if (print)
                {
                    errDrainer.addOutputStream(new OutputFormatAdapter(out));
                    outDrainer.addOutputStream(new OutputFormatAdapter(out));
                }

                errDrainer.start();
                outDrainer.start();

                int ecode = p.waitFor();

                errDrainer.join();
                outDrainer.join();

                if (ecode != 0)
                {
                    out.println("<native image rebuild failed with exit code " + ecode + ">");
                    out.println("<stdout last='" + TAIL_LINES_ON_ERROR + " lines'>");
                    for (String l : FileUtils.tail(stdOut.file(), TAIL_LINES_ON_ERROR))
                    {
                        out.println(l);
                    }
                    out.println("</stdout>");
                    out.println("<stderr last='" + TAIL_LINES_ON_ERROR + " lines'>");
                    for (String l : FileUtils.tail(stdErr.file(), TAIL_LINES_ON_ERROR))
                    {
                        out.println(l);
                    }
                    out.println("</stderr>");

                    out.println("");

                    throw new IllegalStateException("Native image rebuild failed with exit code " + ecode);
                }

            }
            catch (IOException ex)
            {
                out.println("<failed to invoke native-image, caught IOException: " + ex.getMessage() + ">");
                out.println("");
                throw new UncheckedIOException(ex);
            }
            catch (InterruptedException ex)
            {
                out.println("<interrupted waiting for native image: " + ex.getMessage() + ">");
                out.println("");
                throw new RuntimeException(ex);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static class OutputFormatAdapter extends OutputStream
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
}
