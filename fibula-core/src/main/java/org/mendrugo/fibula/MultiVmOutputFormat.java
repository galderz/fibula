package org.mendrugo.fibula;

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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class MultiVmOutputFormat implements OutputFormat
{
    final OutputFormat out;

    MultiVmOutputFormat(Options options)
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
        final ForkedVm forkedVm = ForkedVm.instance(out);
        final BenchmarkParamsReflect reflect = new BenchmarkParamsReflect(benchmark, out);
        reflect.amendField("jvm", forkedVm.executablePath(benchmark.getJvm()));
        reflect.amendField("jdkVersion", forkedVm.jdkVersion());
        reflect.amendField("vmName", forkedVm.vmName());
        reflect.amendField("vmVersion", forkedVm.vmVersion());

        // todo bring back version once this code is independent
        // amendBenchmarkParamsField("jmhVersion", "fibula:" + new Version().getVersion(), benchmark);
        reflect.amendField("jmhVersion", "fibula:999-SNAPSHOT");

        final Collection<String> jvmArgs = benchmark.getJvmArgs();
        if (forkedVm.isNativeVm())
        {
            // Avoid -XX: arguments being passed in to native, because they're not understood in that environment
            System.setProperty("jmh.compilerhints.mode", "FORCE_OFF");

            // Skip jvm arguments that are invalid in native
            final Pattern skip = skipNativeInvalidJvmArgs();
            final List<String> nativeValidJvmArgs = jvmArgs.stream()
                .filter(arg -> !skip.matcher(arg).matches())
                .collect(Collectors.toCollection(ArrayList::new));
            reflect.amendField("jvmArgs", nativeValidJvmArgs);
        }
        else
        {
            final List<String> hotspotJvmArgs = new ArrayList<>(jvmArgs);
            if (Boolean.getBoolean("fibula.native.agent"))
            {
                hotspotJvmArgs.add("-agentlib:native-image-agent=config-output-dir=target/native-agent-config");
            }
            reflect.amendField("jvmArgs", hotspotJvmArgs);
        }
    }

    private static Pattern skipNativeInvalidJvmArgs()
    {
        final List<String> skipJvmArgs = Arrays.asList(
            "-XX:(\\+|-)UnlockExperimentalVMOptions"
            , "-XX:(\\+|-)EnableJVMCIProduct"
            , "-XX:ThreadPriorityPolicy=\\d+"
        );

        final StringJoiner joiner = new StringJoiner("|");
        skipJvmArgs.forEach(joiner::add);
        return Pattern.compile(joiner.toString());
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
