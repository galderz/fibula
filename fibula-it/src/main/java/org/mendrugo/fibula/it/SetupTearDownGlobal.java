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

// todo consider not only invocations but also order, see state order tests
@State(Scope.Benchmark)
public class SetupTearDownGlobal
{
    final AtomicInteger trialCounter = new AtomicInteger();

    @Setup(Level.Trial)
    public void beforeTrial()
    {
        trialCounter.incrementAndGet();
    }

    @TearDown(Level.Trial)
    public void afterTrial()
    {
        assertThat(trialCounter.get(), is(1));
    }

    @Benchmark
    public void bench()
    {
        Work.work();
    }
}
