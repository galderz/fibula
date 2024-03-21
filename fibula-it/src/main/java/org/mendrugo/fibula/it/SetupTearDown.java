package org.mendrugo.fibula.it;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

@State(Scope.Thread)
public class SetupTearDown
{
    final AtomicInteger trialCounter = new AtomicInteger();
    final AtomicInteger beforeIterationCounter = new AtomicInteger();
    final AtomicInteger afterIterationCounter = new AtomicInteger();
    final AtomicInteger beforeInvocationCounter = new AtomicInteger();
    final AtomicInteger afterInvocationCounter = new AtomicInteger();

    @Setup(Level.Trial)
    public void beforeTrial()
    {
        trialCounter.incrementAndGet();
    }

    @Setup(Level.Iteration)
    public void beforeIteration()
    {
        beforeIterationCounter.incrementAndGet();
    }

    @Setup(Level.Invocation)
    public void beforeInvocation()
    {
        beforeInvocationCounter.incrementAndGet();
    }

    @TearDown(Level.Trial)
    public void afterTrial()
    {
        assertThat(trialCounter.get(), is(1));
        assertThat(beforeIterationCounter.get(), is(2));
        assertThat(afterIterationCounter.get(), is(2));
        assertThat(beforeInvocationCounter.get(), greaterThan(2));
        assertThat(afterInvocationCounter.get(), greaterThan(2));
    }

    @TearDown(Level.Iteration)
    public void afterIteration()
    {
        afterIterationCounter.incrementAndGet();
    }

    @TearDown(Level.Invocation)
    public void afterInvocation()
    {
        afterInvocationCounter.incrementAndGet();
    }

    @Benchmark
    public void bench()
    {
        Work.work();
    }
}
