package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.UnCloseablePrintStream;
import org.openjdk.jmh.util.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 * An output format that amends benchmark bootstrap parameters to work with native and JVM modes.
 */
final class BootstrapOutputFormat implements OutputFormat
{
    private final OutputFormat out;

    BootstrapOutputFormat(Options options)
    {
        this.out = createOutputFormat(options);
    }

    private static OutputFormat createOutputFormat(Options options)
    {
        if (options == null)
        {
            throw new IllegalArgumentException("Options not allowed to be null.");
        }

        PrintStream out;
        if (options.getOutput().hasValue())
        {
            try
            {
                out = new PrintStream(options.getOutput().get());
            }
            catch (FileNotFoundException ex)
            {
                throw new IllegalStateException(ex);
            }
        } else
        {
            // Protect the System.out from accidental closing
            try
            {
                out = new UnCloseablePrintStream(System.out, Utils.guessConsoleEncoding());
            }
            catch (UnsupportedEncodingException ex)
            {
                throw new IllegalStateException(ex);
            }
        }

        return OutputFormatFactory.createFormatInstance(out, options.verbosity().orElse(Defaults.VERBOSITY));
    }

    @Override
    public void iteration(BenchmarkParams benchParams, IterationParams params, int iteration)
    {
        out.iteration(benchParams, params, iteration);
    }

    @Override
    public void iterationResult(BenchmarkParams benchParams, IterationParams params, int iteration, IterationResult data)
    {
        out.iterationResult(benchParams, params, iteration, data);
    }

    @Override
    public void startBenchmark(BenchmarkParams benchParams)
    {
        amendBenchmarkParams(benchParams);
        out.startBenchmark(benchParams);
    }

    private void amendBenchmarkParams(BenchmarkParams benchmark)
    {
        final Vm vm = Vm.instance();
        final Vm.Info vmInfo = vm.info(out);
        amendBenchmarkParamsField("jvm", vm.executablePath(benchmark.getJvm()), benchmark);
        amendBenchmarkParamsField("jvmArgs", vm.jvmArgs(benchmark.getJvmArgs()), benchmark);
        amendBenchmarkParamsField("jdkVersion", vmInfo.jdkVersion(), benchmark);
        amendBenchmarkParamsField("vmName", vmInfo.vmName(), benchmark);
        amendBenchmarkParamsField("vmVersion", vmInfo.vmVersion(), benchmark);
        amendBenchmarkParamsField("jmhVersion", "fibula:" + new Version().getVersion(), benchmark);
        if (Vm.SUBSTRATE == vm)
        {
            // Avoid -XX: arguments being passed in to native, because they're not understood in that environment
            System.setProperty("jmh.compilerhints.mode", "FORCE_OFF");
        }
    }

    private void amendBenchmarkParamsField(String fieldName, Object newValue, BenchmarkParams obj)
    {
        try
        {
            final Class<?> clazz = Class.forName("org.openjdk.jmh.infra.BenchmarkParamsL2");
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, newValue);
        }
        catch (Exception e)
        {
            out.println(String.format("Unable to amend benchmark params field %s", fieldName));
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            out.verbosePrintln(stringWriter.toString());
        }
    }

    @Override
    public void endBenchmark(BenchmarkResult result)
    {
        out.endBenchmark(result);
    }

    @Override
    public void startRun()
    {
        out.startRun();
    }

    @Override
    public void endRun(Collection<RunResult> result)
    {
        out.endRun(result);
    }

    @Override
    public void print(String s)
    {
        out.print(s);
    }

    @Override
    public void println(String s)
    {
        out.println(s);
    }

    @Override
    public void flush()
    {
        out.flush();
    }

    @Override
    public void close()
    {
        out.close();
    }

    @Override
    public void verbosePrintln(String s)
    {
        out.verbosePrintln(s);
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
