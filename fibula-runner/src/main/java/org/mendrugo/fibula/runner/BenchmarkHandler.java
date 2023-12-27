package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.NativeIterationResult;
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
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.UnCloseablePrintStream;
import org.openjdk.jmh.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
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

    void runBenchmark(BenchmarkCallable callable, ResultRestClient client, Infrastructure infrastructure)
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

        final int measurementIterations = Integer.parseInt(cli.required("iterations"));
        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , measurementIterations
            , Defaults.MEASUREMENT_TIME
            , Defaults.MEASUREMENT_BATCHSIZE
        );

        for (int i = 1; i <= measurement.getCount(); i++)
        {
            out.iteration(null, measurement, i);
            IterationResult iterationResult = runIteration(callable, measurement, infrastructure);
            out.iterationResult(null, measurement, i, iterationResult);
            client.send(NativeIterationResult.of(iterationResult));
        }
    }

    private IterationResult runIteration(BenchmarkCallable callable, IterationParams iterationParams, Infrastructure infrastructure)
    {
        final List<Result> iterationResults = new ArrayList<>();
        final BenchmarkTaskResult benchmarkTaskResult = runTask(callable, infrastructure);
        iterationResults.addAll(benchmarkTaskResult.getResults());

        long allOps = benchmarkTaskResult.getAllOps();
        long measuredOps = benchmarkTaskResult.getMeasuredOps();

        IterationResult result = new IterationResult(null, iterationParams, new IterationResultMetaData(allOps, measuredOps));
        result.addResults(iterationResults);
        return result;
    }

    private BenchmarkTaskResult runTask(BenchmarkCallable callable, Infrastructure infrastructure)
    {
        final long defaultTimeout = TimeUnit.MINUTES.toNanos(1);
        long waitDeadline = System.nanoTime() + defaultTimeout;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletionService<RawResults> completionService = new ExecutorCompletionService<>(executor);

        final Future<RawResults> completed = completionService.submit(callable);

        // todo read run time from parameters
        // final long defaultRuntime = TimeUnit.SECONDS.toNanos(10);
        final long defaultRuntime = TimeUnit.SECONDS.toNanos(3);

        try
        {
            final Future<RawResults> failing = completionService.poll(defaultRuntime, TimeUnit.NANOSECONDS);
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
