package org.mendrugo.fibula.runner;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.mendrugo.fibula.results.IterationEnd;
import org.mendrugo.fibula.results.IterationStart;

@Path("/output")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "bootstrap")
public interface OutputFormatClient
{
    @Path("/iteration-start")
    @POST
    void iteration(IterationStart iterationStart);

    @Path("/iteration-end")
    @POST
    void iterationResult(IterationEnd iterationEnd);

    @Path("/start-benchmark")
    @POST
    void startBenchmark(String benchParams);

    @Path("/end-benchmark")
    @POST
    void endBenchmark(String benchmarkResult);

    @Path("/start-run")
    @POST
    void startRun();

    @Path("/end-run")
    @POST
    void endRun(String runResults);

    /* ------------- RAW OUTPUT METHODS ------------------- */

    @Path("/print")
    @POST
    void print(String s);

    @Path("/println")
    @POST
    void println(String s);

    @Path("/flush")
    @POST
    void flush();

    /**
     * Renamed from close to closeOutput to avoid clashing with Quarkus:
     * Caused by: java.lang.ClassFormatError: Duplicate method name "close" with signature "()V" in class file org/mendrugo/fibula/runner/OutputFormatClient$$QuarkusRestClientInterface
     */
    @Path("/close-output")
    @POST
    void closeOutput();

    @Path("/verbose")
    @POST
    void verbosePrintln(String s);

    @Path("/write-byte")
    @POST
    void write(int b);

    @Path("/write-bytes")
    @POST
    void write(byte[] b);
}
