package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.Infrastructure;
import org.mendrugo.fibula.results.JmhFormats;
import org.mendrugo.fibula.results.NativeIterationResult;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.AverageTimeResult;
import org.openjdk.jmh.results.BenchmarkTaskResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.IterationResultMetaData;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.runner.format.OutputFormat;

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

    void runBenchmark(BenchmarkCallable callable, ResultRestClient client)
    {
        final OutputFormat out = JmhFormats.outputFormat();
        final BenchmarkParams params = Serializables.fromBase64(cli.text(RunnerArguments.PARAMS));

        final IterationParams warmup = params.getWarmup();
        for (int i = 1; i <= warmup.getCount(); i++)
        {
            out.iteration(params, warmup, i);
            IterationResult iterationResult = runIteration(params, callable, warmup, callable.infrastructure);
            out.iterationResult(params, warmup, i, iterationResult);
        }

        final IterationParams measurement = params.getMeasurement();
        for (int i = 1; i <= measurement.getCount(); i++)
        {
            out.iteration(params, measurement, i);
            IterationResult iterationResult = runIteration(params, callable, measurement, callable.infrastructure);
            out.iterationResult(params, measurement, i, iterationResult);
            client.send(new NativeIterationResult(Serializables.toBase64(iterationResult)));
        }
    }

    private IterationResult runIteration(BenchmarkParams params, BenchmarkCallable callable, IterationParams iterationParams, Infrastructure infrastructure)
    {
        final List<Result> iterationResults = new ArrayList<>();
        final BenchmarkTaskResult benchmarkTaskResult = runTask(callable, infrastructure, iterationParams, params);
        iterationResults.addAll(benchmarkTaskResult.getResults());

        long allOps = benchmarkTaskResult.getAllOps();
        long measuredOps = benchmarkTaskResult.getMeasuredOps();

        IterationResult result = new IterationResult(params, iterationParams, new IterationResultMetaData(allOps, measuredOps));
        result.addResults(iterationResults);
        return result;
    }

    private BenchmarkTaskResult runTask(BenchmarkCallable callable, Infrastructure infrastructure, IterationParams iterationParams, BenchmarkParams benchmarkParams)
    {
        final long defaultTimeout = TimeUnit.MINUTES.toNanos(1);
        long waitDeadline = System.nanoTime() + defaultTimeout;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletionService<RawResults> completionService = new ExecutorCompletionService<>(executor);

        final Future<RawResults> completed = completionService.submit(callable);

        try
        {
            final TimeUnit timeUnit = TimeUnit.NANOSECONDS;
            final Future<RawResults> failing = completionService.poll(iterationParams.getTime().convertTo(timeUnit), timeUnit);
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
            final RawResults res = completed.get();
            infrastructure.resetDone(); // reset done for further iterations

            res.allOps += res.measuredOps;
            final int batchSize = iterationParams.getBatchSize();
            final int opsPerInv = benchmarkParams.getOpsPerInvocation();
            res.allOps *= opsPerInv;
            res.allOps /= batchSize;
            res.measuredOps *= opsPerInv;
            res.measuredOps /= batchSize;
            final BenchmarkTaskResult results = new BenchmarkTaskResult((long) res.allOps, (long) res.measuredOps);
            final Result result = switch (benchmarkParams.getMode())
            {
                case Throughput -> new ThroughputResult(ResultRole.PRIMARY, "tbd", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit());
                case AverageTime -> new AverageTimeResult(ResultRole.PRIMARY, "tbd", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit());
                default -> throw new RuntimeException("NYI");
            };

            results.add(result);
            return results;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
