package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.NativeBenchmarkParams;
import org.mendrugo.fibula.results.NativeIterationResult;
import org.mendrugo.fibula.results.RunnerArguments;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkTaskResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.IterationResultMetaData;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.UnCloseablePrintStream;
import org.openjdk.jmh.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class BenchmarkHandler
{
    private final Cli cli;

    public BenchmarkHandler(Cli cli)
    {
        this.cli = cli;
    }

    void runBenchmark(BenchmarkCallable callable, ResultRestClient client)
    {
        // todo move it to a common module
        final OutputFormat out;
        try
        {
            final UnCloseablePrintStream printStream = new UnCloseablePrintStream(System.out, Utils.guessConsoleEncoding());
            out = OutputFormatFactory.createFormatInstance(printStream, VerboseMode.NORMAL);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException(e);
        }

        final NativeBenchmarkParams params = callable.infrastructure.getBenchmarkParams();

        final int warmupIterations = params.getWarmupIterations(cli.integer(RunnerArguments.WARMUP_ITERATIONS));
        final Optional<TimeValue> cmdLineValue = cli.timeValue(RunnerArguments.WARMUP_TIME);
        final TimeValue warmupTime = params.getWarmupTime(cmdLineValue);
        final IterationParams warmup = new IterationParams(
            IterationType.WARMUP
            , warmupIterations
            , warmupTime
            , Defaults.WARMUP_BATCHSIZE
        );
        for (int i = 1; i <= warmup.getCount(); i++)
        {
            out.iteration(null, warmup, i);
            IterationResult iterationResult = runIteration(callable, warmup, callable.infrastructure);
            out.iterationResult(null, warmup, i, iterationResult);
        }

        final int measurementIterations = params.getMeasurementIterations(cli.integer(RunnerArguments.MEASUREMENT_ITERATIONS));
        final TimeValue measurementTime = params.getMeasurementTime(cli.timeValue(RunnerArguments.MEASUREMENT_TIME));
        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , measurementIterations
            , measurementTime
            , Defaults.MEASUREMENT_BATCHSIZE
        );

        for (int i = 1; i <= measurement.getCount(); i++)
        {
            out.iteration(null, measurement, i);
            IterationResult iterationResult = runIteration(callable, measurement, callable.infrastructure);
            out.iterationResult(null, measurement, i, iterationResult);
            client.send(NativeIterationResult.of(iterationResult, params.getAnnotationParams()));
        }
    }

    private IterationResult runIteration(BenchmarkCallable callable, IterationParams iterationParams, Infrastructure infrastructure)
    {
        final List<Result> iterationResults = new ArrayList<>();
        final BenchmarkTaskResult benchmarkTaskResult = runTask(callable, infrastructure, iterationParams.getTime());
        iterationResults.addAll(benchmarkTaskResult.getResults());

        long allOps = benchmarkTaskResult.getAllOps();
        long measuredOps = benchmarkTaskResult.getMeasuredOps();

        IterationResult result = new IterationResult(null, iterationParams, new IterationResultMetaData(allOps, measuredOps));
        result.addResults(iterationResults);
        return result;
    }

    private BenchmarkTaskResult runTask(BenchmarkCallable callable, Infrastructure infrastructure, TimeValue runTime)
    {
        final long defaultTimeout = TimeUnit.MINUTES.toNanos(1);
        long waitDeadline = System.nanoTime() + defaultTimeout;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletionService<RawResults> completionService = new ExecutorCompletionService<>(executor);

        final Future<RawResults> completed = completionService.submit(callable);

        try
        {
            final TimeUnit timeUnit = TimeUnit.NANOSECONDS;
            final Future<RawResults> failing = completionService.poll(runTime.convertTo(timeUnit), timeUnit);
            if (failing != null)
            {
                System.out.println("Benchmark finished before it was due!");
            }
        }
        catch (InterruptedException e)
        {
            // Ignore
        }

        infrastructure.markDone();

        try
        {
            final RawResults raw = completed.get();
            infrastructure.resetDone(); // reset done for further iterations

            raw.allOps += raw.measuredOps;
            final int batchSize = 1; // todo iteration param
            final int opsPerInv = 1; // todo bench param
            raw.measuredOps *= opsPerInv;
            raw.measuredOps /= batchSize;
            final BenchmarkTaskResult results = new BenchmarkTaskResult((long) raw.allOps, (long) raw.measuredOps);
            final TimeUnit timeUnit = TimeUnit.SECONDS; // todo bench param
            results.add(new ThroughputResult(ResultRole.PRIMARY, "tbd", raw.measuredOps, raw.getTime(), timeUnit));
            return results;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
