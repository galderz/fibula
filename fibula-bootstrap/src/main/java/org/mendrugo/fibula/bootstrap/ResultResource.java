package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.NativeBenchmarkTaskResult;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.IterationResultMetaData;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.UnCloseablePrintStream;
import org.openjdk.jmh.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Path("/results")
public class ResultResource
{
    @POST
    public String add(NativeBenchmarkTaskResult result)
    {
        Log.infof("Received result: %s", result);
        // new ResultFormat().write(result);
        endRun(result);
        Log.infof("Now exit the application");
        Quarkus.asyncExit();
        return "Ok";
    }

    private void endRun(NativeBenchmarkTaskResult result)
    {
        final Collection<RunResult> runResults = runResults(result);
        try
        {
            final UnCloseablePrintStream out = new UnCloseablePrintStream(System.out, Utils.guessConsoleEncoding());
            ResultFormatFactory.getInstance(ResultFormatType.TEXT, out).writeOut(runResults);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private Collection<RunResult> runResults(NativeBenchmarkTaskResult result)
    {
        final BenchmarkParams benchmarkParams = getBenchmarkParams();
        final Collection<BenchmarkResult> results = benchmarkResults(result);
        return List.of(new RunResult(benchmarkParams, results));
    }

    private Collection<BenchmarkResult> benchmarkResults(NativeBenchmarkTaskResult result)
    {
        final BenchmarkParams benchmarkParams = getBenchmarkParams();
        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , 1 // Defaults.MEASUREMENT_ITERATIONS
            , Defaults.MEASUREMENT_TIME
            , Defaults.MEASUREMENT_BATCHSIZE
        );
        final IterationResult iterationResult = new IterationResult(benchmarkParams, measurement, new IterationResultMetaData(result.allOperations(), result.measuredOperations()));
        iterationResult.addResult(Results.toResult(result.results().iterator().next()));
        return List.of(new BenchmarkResult(benchmarkParams, List.of(iterationResult)));
    }

    private BenchmarkParams getBenchmarkParams()
    {
        final IterationParams warmup = new IterationParams(
            IterationType.WARMUP
            , 0 // Defaults.WARMUP_ITERATIONS
            , Defaults.WARMUP_TIME
            , Defaults.WARMUP_BATCHSIZE
        );
        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , 1 // Defaults.MEASUREMENT_ITERATIONS
            , Defaults.MEASUREMENT_TIME
            , Defaults.MEASUREMENT_BATCHSIZE
        );
        final WorkloadParams params = new WorkloadParams();

        String jdkVersion = System.getProperty("java.version");
        String vmVersion = System.getProperty("java.vm.version");
        String vmName = System.getProperty("java.vm.name");

        return new BenchmarkParams(
            "org.mendrugo.fibula.samples.FibulaSample_01_HelloWorld.helloWorld"
            , ""
            , true
            , 1
            , new int[]{1}
            , Collections.emptyList()
            , 1
            , 0
            , warmup
            , measurement
            , Mode.Throughput
            , params
            , TimeUnit.SECONDS
            , 1
            , ""
            , new ArrayList<>()
            , jdkVersion
            , vmName
            , vmVersion
            , "0.1"
            , TimeValue.minutes(10)
        );
    }
}
