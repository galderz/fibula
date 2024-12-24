package org.mendrugo.fibula;

import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.TempFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

final class Execute
{
    private static final int TAIL_LINES_ON_ERROR = Integer.getInteger("jmh.tailLines", 20);

    private final boolean print;
    private final Consumer<String> println;
    private final Consumer<Integer> writeInt;
    private final Consumer<byte[]> writeBytes;

    private Execute(boolean print, Consumer<String> println, Consumer<Integer> writeInt, Consumer<byte[]> writeBytes) {
        this.print = print;
        this.println = println;
        this.writeInt = writeInt;
        this.writeBytes = writeBytes;
    }

    static Execute from(boolean print, PrintStream out)
    {
        final Consumer<byte[]> writeBytes = bytes -> {
            try
            {
                out.write(bytes);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        };
        return new Execute(print, out::println, out::write, writeBytes);
    }

    static Execute from(boolean print, OutputFormat out)
    {
        final Consumer<byte[]> writeBytes = bytes -> {
            try
            {
                out.write(bytes);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        };
        return new Execute(print, out::println, out::write, writeBytes);
    }

    Collection<String> execute(List<String> commandString)
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
                    errDrainer.addOutputStream(new OutputFormatAdapter(writeInt, writeBytes));
                    outDrainer.addOutputStream(new OutputFormatAdapter(writeInt, writeBytes));
                }

                errDrainer.start();
                outDrainer.start();

                int ecode = p.waitFor();

                errDrainer.join();
                outDrainer.join();

                if (ecode != 0)
                {
                    println.accept("<native image rebuild failed with exit code " + ecode + ">");
                    println.accept("<stdout last='" + TAIL_LINES_ON_ERROR + " lines'>");
                    for (String l : FileUtils.tail(stdOut.file(), TAIL_LINES_ON_ERROR))
                    {
                        println.accept(l);
                    }
                    println.accept("</stdout>");
                    println.accept("<stderr last='" + TAIL_LINES_ON_ERROR + " lines'>");
                    for (String l : FileUtils.tail(stdErr.file(), TAIL_LINES_ON_ERROR))
                    {
                        println.accept(l);
                    }
                    println.accept("</stderr>");

                    println.accept("");

                    throw new IllegalStateException("Native image rebuild failed with exit code " + ecode);
                }

                return Files.readAllLines(stdOut.file().toPath());
            }
            catch (IOException ex)
            {
                println.accept("<failed to invoke native-image, caught IOException: " + ex.getMessage() + ">");
                println.accept("");
                throw new UncheckedIOException(ex);
            }
            catch (InterruptedException ex)
            {
                println.accept("<interrupted waiting for native image: " + ex.getMessage() + ">");
                println.accept("");
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
        private final Consumer<Integer> writeInt;
        private final Consumer<byte[]> writeBytes;

        public OutputFormatAdapter(Consumer<Integer> writeInt, Consumer<byte[]> writeBytes)
        {
            this.writeInt = writeInt;
            this.writeBytes = writeBytes;
        }

        @Override
        public void write(int b)
        {
            writeInt.accept(b);
        }

        @Override
        public void write(byte[] b)
        {
            writeBytes.accept(b);
        }
    }
}
