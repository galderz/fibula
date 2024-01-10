package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.JmhFormats;
import org.mendrugo.fibula.results.NativeBenchmarkParams;
import org.mendrugo.fibula.results.NativeIterationResult;
import org.mendrugo.fibula.results.RunnerArguments;
import org.mendrugo.fibula.results.Serializables;
import org.openjdk.jmh.infra.BenchmarkParams;
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
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.Serializable;
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
        final OutputFormat out = JmhFormats.outputFormat();
        // final NativeBenchmarkParams params = callable.infrastructure.getBenchmarkParams();

        final BenchmarkParams params = Serializables.fromBase64(cli.text(RunnerArguments.PARAMS));

//        final int warmupIterations = params.getWarmupIterations(cli.integerOpt(RunnerArguments.WARMUP_ITERATIONS));
//        final Optional<TimeValue> cmdLineValue = cli.timeValueOpt(RunnerArguments.WARMUP_TIME);
//        final TimeValue warmupTime = params.getWarmupTime(cmdLineValue);
//        final IterationParams warmup = new IterationParams(
//            IterationType.WARMUP
//            , warmupIterations
//            , warmupTime
//            , Defaults.WARMUP_BATCHSIZE
//        );
        final IterationParams warmup = params.getWarmup();
        for (int i = 1; i <= warmup.getCount(); i++)
        {
            out.iteration(null, warmup, i);
            IterationResult iterationResult = runIteration(callable, warmup, callable.infrastructure);
            out.iterationResult(null, warmup, i, iterationResult);
        }

        final IterationParams measurement = params.getMeasurement();
        for (int i = 1; i <= measurement.getCount(); i++)
        {
            out.iteration(null, measurement, i);
            IterationResult iterationResult = runIteration(callable, measurement, callable.infrastructure);
            out.iterationResult(null, measurement, i, iterationResult);
            client.send(NativeIterationResult.of(iterationResult, callable.infrastructure.getBenchmark()));
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
