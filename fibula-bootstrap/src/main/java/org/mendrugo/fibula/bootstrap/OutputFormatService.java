package org.mendrugo.fibula.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.IOException;
import java.util.Collection;

@ApplicationScoped
public class OutputFormatService implements OutputFormat
{
    private final OutputFormat out;

    public OutputFormatService()
    {
        this.out = JmhFormats.outputFormat();
    }

//    OutputFormat output()
//    {
//        return this.out;
//    }

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
        out.startBenchmark(benchParams);
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