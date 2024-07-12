package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.BenchmarkTaskResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.IterationResultMetaData;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.runner.BenchmarkException;
import org.openjdk.jmh.runner.InfraControl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class BenchmarkHandler
{
    private final IterationClient iterationClient;
    private final BenchmarkParams benchmarkParams;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ConcurrentMap<Thread, WorkerData> workerMap = new ConcurrentHashMap<>();
    private final CyclicBarrier workerDataBarrier;
    private final BlockingQueue<WorkerData> unusedWorkerData;

    public BenchmarkHandler(IterationClient iterationClient, BenchmarkParams benchmarkParams)
    {
        this.iterationClient = iterationClient;
        this.benchmarkParams = benchmarkParams;

        int threads = benchmarkParams.getThreads();
        this.workerDataBarrier = new CyclicBarrier(threads, this::captureUnusedWorkerData);
        this.unusedWorkerData = new ArrayBlockingQueue<>(threads);
    }

    void runBenchmark(BenchmarkSupplier benchmark)
    {
        long allWarmup = 0;
        long allMeasurement = 0;

        final IterationParams warmup = benchmarkParams.getWarmup();
        final long warmupTime = System.currentTimeMillis();
        for (int i = 1; i <= warmup.getCount(); i++)
        {
            iterationClient.notifyStart(new IterationStart(Serializables.toBase64(benchmarkParams), Serializables.toBase64(warmup), i));
            final boolean isFirstIteration = (i == 1);
            final boolean isLastIteration = i == warmup.getCount();
            IterationResult iterationResult = runIteration(
                benchmark
                , warmup
                , benchmarkParams
                , isFirstIteration
                , isLastIteration
            );
            iterationClient.notifyEnd(new IterationEnd(i, Serializables.toBase64(iterationResult)));
            allWarmup += iterationResult.getMetadata().getAllOps();
        }

        IterationParams measurement = benchmarkParams.getMeasurement();
        final long measurementTime = System.currentTimeMillis();
        for (int i = 1; i <= measurement.getCount(); i++)
        {
            iterationClient.notifyStart(new IterationStart(Serializables.toBase64(benchmarkParams), Serializables.toBase64(measurement), i));
            final boolean isFirstIteration = (i == 1);
            final boolean isLastIteration = i == measurement.getCount();
            IterationResult iterationResult = runIteration(
                benchmark
                , measurement
                , benchmarkParams
                , isFirstIteration
                , isLastIteration
            );
            iterationClient.notifyEnd(new IterationEnd(i, Serializables.toBase64(iterationResult)));
            allMeasurement += iterationResult.getMetadata().getAllOps();
        }
        final long stopTime = System.currentTimeMillis();

        BenchmarkResultMetaData resultMetaData = new BenchmarkResultMetaData(
            warmupTime
            , measurementTime
            , stopTime
            , allWarmup
            , allMeasurement
        );
        iterationClient.notifyTelemetry(new IterationTelemetry(Serializables.toBase64(resultMetaData)));
    }

    private void captureUnusedWorkerData() {
        unusedWorkerData.addAll(workerMap.values());
        workerMap.clear();
    }

    public void shutdown()
    {
        workerMap.clear();
        executor.shutdownNow();
    }

    private IterationResult runIteration(
        BenchmarkSupplier benchmark
        , IterationParams iterationParams
        , BenchmarkParams benchmarkParams
        , boolean isFirstIteration
        , boolean isLastIteration
    )
    {
        final List<Result> iterationResults = new ArrayList<>();
        final BenchmarkTaskResult benchmarkTaskResult = runTask(
            benchmark
            , iterationParams
            , benchmarkParams
            , isFirstIteration
            , isLastIteration
        );
        iterationResults.addAll(benchmarkTaskResult.getResults());

        long allOps = benchmarkTaskResult.getAllOps();
        long measuredOps = benchmarkTaskResult.getMeasuredOps();

        IterationResult result = new IterationResult(
            benchmarkParams
            , iterationParams
            , new IterationResultMetaData(allOps, measuredOps)
        );
        result.addResults(iterationResults);
        return result;
    }

    private BenchmarkTaskResult runTask(
        BenchmarkSupplier benchmark
        , IterationParams iterationParams
        , BenchmarkParams benchmarkParams
        , boolean isFirstIteration
        , boolean isLastIteration
    )
    {
        final int numThreads = 1;
        final CountDownLatch preSetupBarrier = new CountDownLatch(numThreads);
        final CountDownLatch preTearDownBarrier = new CountDownLatch(numThreads);
        final boolean shouldYield = false;
        final InfraControl control = new InfraControl(
            benchmarkParams
            , iterationParams
            , preSetupBarrier
            , preTearDownBarrier
            , isFirstIteration
            , isLastIteration
            , shouldYield
            , new Control()
        );
        final ThreadParams threadParams = new ThreadParams(
            0
            , 1
            , 0
            , 1
            , 0
            , 1
            , 0
            , 1
            , 0
            , 1
        );

        final long defaultTimeout = TimeUnit.MINUTES.toNanos(1);
        long waitDeadline = System.nanoTime() + defaultTimeout;

        final CompletionService<BenchmarkTaskResult> completionService = new ExecutorCompletionService<>(executor);

        final BenchmarkCallable callable = new BenchmarkCallable(benchmark, control, threadParams);
        final Future<BenchmarkTaskResult> completed = completionService.submit(callable);

        try
        {
            final TimeUnit timeUnit = TimeUnit.NANOSECONDS;
            completionService.poll(iterationParams.getTime().convertTo(timeUnit), timeUnit);
        }
        catch (InterruptedException e)
        {
            // Ignore
        }

        // now we communicate all worker threads should stop
        control.announceDone();

        // wait for all workers to transit to teardown
        // control.awaitWarmdownReady();

        BenchmarkTaskResult results = null;
        final List<Throwable> errors = new ArrayList<>();
        try
        {
            results = completed.get();
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

    final class BenchmarkCallable implements Callable<BenchmarkTaskResult>
    {
        final BenchmarkSupplier benchmark;
        final InfraControl control;
        final ThreadParams threadParams;

        private volatile Thread runner;

        public BenchmarkCallable(
            BenchmarkSupplier benchmark
            , InfraControl control
            , ThreadParams threadParams
        )
        {
            this.benchmark = benchmark;
            this.control = control;
            this.threadParams = threadParams;
        }

        @Override
        public BenchmarkTaskResult call() throws Exception
        {
            runner = Thread.currentThread();
            WorkerData workerData = control.firstIteration ? newWorkerData(runner) : getWorkerData(runner);
            return benchmark.get().apply(control, workerData);
        }

        private WorkerData getWorkerData(Thread thread) throws Exception
        {
            WorkerData workerData = workerMap.remove(thread);
            workerDataBarrier.await();
            if (workerData == null)
            {
                workerData = unusedWorkerData.poll();
                if (workerData == null)
                {
                    throw new IllegalStateException("Cannot get another thread working data");
                }
            }

            final WorkerData exist = workerMap.put(thread, workerData);
            if (exist != null)
            {
                throw new IllegalStateException("Duplicate thread data");
            }
            return workerData;
        }

        private WorkerData newWorkerData(Thread thread)
        {
            final WorkerData workerData = new WorkerData(benchmark.newInstance(), threadParams);
            workerMap.put(thread, workerData);
            return workerData;
        }
    }
}
