package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.*;
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
import org.openjdk.jmh.runner.BenchmarkException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class BenchmarkHandler
{
    private final IterationClient iterationClient;

    public BenchmarkHandler(IterationClient iterationClient)
    {
        this.iterationClient = iterationClient;
    }

    void runBenchmark(BenchmarkParams params, BenchmarkCallable callable)
    {
        runBenchmarkCount(params.getWarmup(), params, callable);
        runBenchmarkCount(params.getMeasurement(), params, callable);
    }

    private void runBenchmarkCount(IterationParams iterationParams, BenchmarkParams params, BenchmarkCallable callable)
    {
        for (int i = 1; i <= iterationParams.getCount(); i++)
        {
            iterationClient.notifyStart(new IterationStart(Serializables.toBase64(params), Serializables.toBase64(iterationParams), i));
            final boolean lastIteration = i == iterationParams.getCount();
            if (lastIteration)
            {
                callable.infrastructure.markLastIteration();
            }
            IterationResult iterationResult = runIteration(params, callable, iterationParams, callable.infrastructure);
            if (lastIteration)
            {
                callable.infrastructure.resetLastIteration();
            }
            iterationClient.notifyEnd(new IterationEnd(i, Serializables.toBase64(iterationResult)));
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
            completionService.poll(iterationParams.getTime().convertTo(timeUnit), timeUnit);
        }
        catch (InterruptedException e)
        {
            // Ignore
        }

        infrastructure.markDone();

        BenchmarkTaskResult results = null;
        final List<Throwable> errors = new ArrayList<>();
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
            results = new BenchmarkTaskResult((long) res.allOps, (long) res.measuredOps);
            final Result result = switch (benchmarkParams.getMode())
            {
                case Throughput -> new ThroughputResult(ResultRole.PRIMARY, "tbd", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit());
                case AverageTime -> new AverageTimeResult(ResultRole.PRIMARY, "tbd", res.measuredOps, res.getTime(), benchmarkParams.getTimeUnit());
                default -> throw new RuntimeException("NYI");
            };

            results.add(result);
        }
        catch (ExecutionException ex)
        {
            Throwable cause = ex.getCause();
            // todo skip recording assist exception
            errors.add(cause);
        }
        catch (InterruptedException ex)
        {
            throw new BenchmarkException(ex);
        }

        if (!errors.isEmpty())
        {
            throw new BenchmarkException("Benchmark error during the run", errors);
        }

        return results;
    }
}
