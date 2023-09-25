package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.ThroughputResult;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Handler
{
    private final Infrastructure infrastructure;

    public Handler(Infrastructure infrastructure)
    {
        this.infrastructure = infrastructure;
    }

    ThroughputResult runIteration(Callable<ThroughputResult> callable)
    {
        final long defaultTimeout = TimeUnit.MINUTES.toNanos(1);
        long waitDeadline = System.nanoTime() + defaultTimeout;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CompletionService<ThroughputResult> completionService = new ExecutorCompletionService<>(executor);

        final Future<ThroughputResult> completed = completionService.submit(callable);
        final long defaultRuntime = TimeUnit.SECONDS.toNanos(10);
        try
        {
            final Future<ThroughputResult> failing = completionService.poll(defaultRuntime, TimeUnit.NANOSECONDS);
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
            return completed.get();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
