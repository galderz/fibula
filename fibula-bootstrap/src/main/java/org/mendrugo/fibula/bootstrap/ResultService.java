package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import jakarta.enterprise.context.ApplicationScoped;
import org.mendrugo.fibula.results.NativeIterationResult;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
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

@ApplicationScoped
public class ResultService
{
    private final List<NativeIterationResult> iterationResults = new ArrayList<>();

    private NativeOptions options;
    private int forkCount;
    private int iterationCount;
    private ProcessRunner processRunner;

    void addIteration(NativeIterationResult result)
    {
        iterationResults.add(result);
        final int totalIterations = options.getMeasurementForks() * options.getMeasurementIterations();
        if (totalIterations == iterationResults.size())
        {
            endRun(iterationResults);
            Log.infof("Now exit the application");
            Quarkus.asyncExit();
        }

        iterationCount++;
        if (iterationCount == options.getMeasurementIterations())
        {
            forkCount++;
            // Run subsequent forks
            processRunner.runFork(forkCount);
        }
    }

    void setOptions(NativeOptions options)
    {
        this.options = options;
    }

    public void setProcessRunner(ProcessRunner processRunner)
    {
        this.processRunner = processRunner;
    }

    private void endRun(List<NativeIterationResult> results)
    {
        final Collection<RunResult> runResults = runResults(results);
        try
        {
            // todo move it to a common module
            final UnCloseablePrintStream out = new UnCloseablePrintStream(System.out, Utils.guessConsoleEncoding());
            ResultFormatFactory.getInstance(ResultFormatType.TEXT, out).writeOut(runResults);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private Collection<RunResult> runResults(List<NativeIterationResult> results)
    {
        final BenchmarkParams benchmarkParams = getBenchmarkParams();
        final Collection<BenchmarkResult> benchmarkResults = List.of(benchmarkResult(results));
        return List.of(new RunResult(benchmarkParams, benchmarkResults));
    }

    private BenchmarkResult benchmarkResult(List<NativeIterationResult> results)
    {
        final BenchmarkParams benchmarkParams = getBenchmarkParams();
        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , options.getMeasurementIterations()
            , Defaults.MEASUREMENT_TIME
            , Defaults.MEASUREMENT_BATCHSIZE
        );
        final List<IterationResult> iterationResults = results.stream()
            .map(iterationResult -> Results.toIterationResult(iterationResult, benchmarkParams, measurement))
            .toList();
        return new BenchmarkResult(benchmarkParams, iterationResults);
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
            , options.getMeasurementIterations()
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
            , options.getMeasurementForks()
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
