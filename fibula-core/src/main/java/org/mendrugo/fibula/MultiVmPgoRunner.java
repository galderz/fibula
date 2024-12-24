package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.TempFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

final class MultiVmPgoRunner extends Runner
{
    private static final int TAIL_LINES_ON_ERROR = Integer.getInteger("jmh.tailLines", 20);

    boolean instrumentRunCompleted;

    public MultiVmPgoRunner(CommandLineOptions options)
    {
        super(options, new MultiVmOutputFormat(options));
    }

    @Override
    protected void etaBeforeBenchmark()
    {
        super.etaBeforeBenchmark();

        if (!instrumentRunCompleted)
        {
            out.println("# PGO: Instrumented Warmup Fork");
        }
    }

    @Override
    protected void etaAfterBenchmark(BenchmarkParams benchmarkParams)
    {
        super.etaAfterBenchmark(benchmarkParams);

        // The end of the very first run is the end instrumentation run.
        // Rebuild the native binary before executing next fork.
        if (!instrumentRunCompleted)
        {
            instrumentRunCompleted = true;

            boolean forcePrint = options.verbosity()
                .orElse(Defaults.VERBOSITY)
                .equalsOrHigherThan(VerboseMode.EXTRA);

            addProfileInformationToBundle(forcePrint);

            // todo add any debugging/profiling parameters here?
            rebuildNativeExecutable(forcePrint);

            updateExecutablePath(benchmarkParams);
        }
    }

    private void updateExecutablePath(BenchmarkParams benchmarkParams)
    {
        final ForkedVm forkedVm = ForkedVm.instance();
        final BenchmarkParamsReflect reflect = new BenchmarkParamsReflect(benchmarkParams, out);
        reflect.amendField("jvm", forkedVm.executablePath(benchmarkParams.getJvm()));
    }

    private void rebuildNativeExecutable(boolean forcePrint)
    {
        try
        {
            final TempFile stdErr = FileUtils.weakTempFile("stderr");
            final TempFile stdOut = FileUtils.weakTempFile("stdout");

            final List<String> command = List.of(
                NativeImage.INSTANCE.executable.getAbsolutePath()
                , "--bundle-apply=" + Pgo.ENABLED.bundleOptimized.getAbsolutePath()
            );

            out.println("# PGO: Rebuild native from bundle");
            doNativeImage(command, stdOut.file(), stdErr.file(), forcePrint);

            stdOut.delete();
            stdErr.delete();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void addProfileInformationToBundle(boolean forcePrint)
    {
        try
        {
            final TempFile stdErr = FileUtils.weakTempFile("stderr");
            final TempFile stdOut = FileUtils.weakTempFile("stdout");

            final List<String> command = List.of(
                NativeImage.INSTANCE.executable.getAbsolutePath()
                , "--bundle-apply=" + Pgo.ENABLED.bundle.getAbsolutePath()
                , "--bundle-create=" + Pgo.ENABLED.bundleOptimized.getAbsolutePath() + ",dry-run"
                , "--pgo"
            );
            out.println("");
            out.println("# PGO: Rebuild bundle with profiling data");
            doNativeImage(command, stdOut.file(), stdErr.file(), forcePrint);

            stdOut.delete();
            stdErr.delete();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void doNativeImage(
        List<String> commandString
        , File stdOut
        , File stdErr
        , boolean print
    )
    {
        try (FileOutputStream fosErr = new FileOutputStream(stdErr)
             ; FileOutputStream fosOut = new FileOutputStream(stdOut)
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
                for (String l : FileUtils.tail(stdOut, TAIL_LINES_ON_ERROR))
                {
                    out.println(l);
                }
                out.println("</stdout>");
                out.println("<stderr last='" + TAIL_LINES_ON_ERROR + " lines'>");
                for (String l : FileUtils.tail(stdErr, TAIL_LINES_ON_ERROR))
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
