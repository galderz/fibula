package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.IterationEnd;
import org.mendrugo.fibula.results.IterationStart;
import org.mendrugo.fibula.results.Serializables;

import java.io.IOException;
import java.io.UncheckedIOException;

@Path("/output")
public class OutputFormatResource
{
    @Inject
    OutputFormatService out;

    @Path("/iteration-start")
    @POST
    public void iteration(IterationStart iterationStart)
    {
        Log.debugf("Received: %s", iterationStart);
        out.iteration(
            Serializables.fromBase64(iterationStart.benchmarkParams())
            , Serializables.fromBase64(iterationStart.iterationParams())
            , iterationStart.iteration()
        );
    }

    @Path("/iteration-end")
    @POST
    public void iterationResult(IterationEnd iterationEnd)
    {
        Log.debugf("Received: %s", iterationEnd);
        out.iterationResult(
            Serializables.fromBase64(iterationEnd.benchmarkParams())
            , Serializables.fromBase64(iterationEnd.iterationParams())
            , iterationEnd.iteration()
            , Serializables.fromBase64(iterationEnd.iterationResult())
        );
    }

    @Path("/start-benchmark")
    @POST
    public void startBenchmark(String benchParams)
    {
        Log.debugf("Received: start-benchmark benchmarkParams=%s", benchParams);
        out.startBenchmark(Serializables.fromBase64(benchParams));
    }

    @Path("/end-benchmark")
    @POST
    public void endBenchmark(String benchmarkResult)
    {
        Log.debugf("Received: end-benchmark benchmarkResult=%s", benchmarkResult);
        out.endBenchmark(Serializables.fromBase64(benchmarkResult));
    }

    @Path("/start-run")
    @POST
    public void startRun()
    {
        Log.debugf("Received: start-run");
        out.startRun();
    }

    @Path("/end-run")
    @POST
    public void endRun(String runResults)
    {
        Log.debugf("Received: end-run runResults=%s", runResults);
        out.endRun(Serializables.fromBase64(runResults));
    }

    /* ------------- RAW OUTPUT METHODS ------------------- */

    @Path("/print")
    @POST
    public void print(String s)
    {
        Log.debugf("Received: print %s", s);
        out.print(s);
    }

    @Path("/println")
    @POST
    public void println(String s)
    {
        Log.debugf("Received: println %s", s);
        out.println(s);
    }

    @Path("/flush")
    @POST
    public void flush()
    {
        Log.debugf("Received: flush");
        out.flush();
    }

    @Path("/close")
    @POST
    public void close()
    {
        Log.debugf("Received: close");
        out.close();
    }

    @Path("/verbose")
    @POST
    public void verbosePrintln(String s)
    {
        Log.debugf("Received: verbosePrintln %s", s);
        out.verbosePrintln(s);
    }

    @Path("/write-byte")
    @POST
    public void write(int b)
    {
        Log.debugf("Received: write %s", b);
        out.write(b);
    }

    @Path("/write-bytes")
    @POST
    public void write(byte[] b)
    {
        Log.debugf("Received: write %s", b);
        try
        {
            out.write(b);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
