package org.mendrugo.fibula.runner;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mendrugo.fibula.results.IterationEnd;
import org.mendrugo.fibula.results.IterationStart;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

@ApplicationScoped
public class OutputFormatDelegate implements OutputFormat
{
    @RestClient
    OutputFormatClient outputClient;

    @Override
    public void iteration(BenchmarkParams benchParams, IterationParams params, int iteration)
    {
        outputClient.iteration(new IterationStart(
            Serializables.toBase64(benchParams)
            , Serializables.toBase64(params)
            , iteration
        ));
    }

    @Override
    public void iterationResult(BenchmarkParams benchParams, IterationParams params, int iteration, IterationResult data)
    {
        outputClient.iterationResult(new IterationEnd(
            Serializables.toBase64(benchParams)
            , Serializables.toBase64(params)
            , iteration
            , Serializables.toBase64(data)
        ));
    }

    @Override
    public void startBenchmark(BenchmarkParams benchParams)
    {
        outputClient.startBenchmark(Serializables.toBase64(benchParams));
    }

    @Override
    public void endBenchmark(BenchmarkResult result)
    {
        outputClient.endBenchmark(Serializables.toBase64(result));
    }

    @Override
    public void startRun()
    {
        outputClient.startRun();
    }

    @Override
    public void endRun(Collection<RunResult> result)
    {
        outputClient.endRun(Serializables.toBase64((Serializable) result));
    }

    @Override
    public void print(String s)
    {
        outputClient.print(s);
    }

    @Override
    public void println(String s)
    {
        outputClient.println(s);
    }

    @Override
    public void flush()
    {
        outputClient.flush();
    }

    @Override
    public void close()
    {
        outputClient.closeOutput();
    }

    @Override
    public void verbosePrintln(String s)
    {
        outputClient.verbosePrintln(s);
    }

    @Override
    public void write(int b)
    {
        outputClient.write(b);
    }

    @Override
    public void write(byte[] b)
    {
        outputClient.write(b);
    }
}
