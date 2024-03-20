package org.mendrugo.fibula.it;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@State(Scope.Thread)
public class SetupTearDown
{
    final AtomicInteger aroundTrialCounter = new AtomicInteger();

    @Setup(Level.Trial)
    public void beforeTrial()
    {
        aroundTrialCounter.incrementAndGet();
    }

    @TearDown(Level.Trial)
    public void afterTrial()
    {
        assertThat(aroundTrialCounter.get(), is(1));
    }

    @Benchmark
    public void bench()
    {
        Work.work();
    }
}
