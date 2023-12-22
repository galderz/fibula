package org.mendrugo.fibula.runner;

import org.openjdk.jmh.results.BenchmarkTaskResult;
import org.openjdk.jmh.results.RawResults;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BenchmarkHandler
{
    BenchmarkTaskResult handle(BenchmarkCallable callable, Infrastructure infrastructure)
    {
        final long defaultTimeout = TimeUnit.MINUTES.toNanos(1);
        long waitDeadline = System.nanoTime() + defaultTimeout;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletionService<RawResults> completionService = new ExecutorCompletionService<>(executor);

        final Future<RawResults> completed = completionService.submit(callable);

        // final long defaultRuntime = TimeUnit.SECONDS.toNanos(10);
        final long defaultRuntime = TimeUnit.SECONDS.toNanos(3);

        try
        {
            final Future<RawResults> failing = completionService.poll(defaultRuntime, TimeUnit.NANOSECONDS);
            if (failing != null)
            {
                System.out.println("Failed!");
            }
        }
        catch (InterruptedException e)
        {
            // Ignore
        }

        System.out.println("Mark done");
        infrastructure.markDone();

        try
        {
            final RawResults raw = completed.get();
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
