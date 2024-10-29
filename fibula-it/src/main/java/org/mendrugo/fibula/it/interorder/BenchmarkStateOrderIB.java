package org.mendrugo.fibula.it.interorder;

import org.junit.Assert;
import org.mendrugo.fibula.it.Work;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkStateOrderIB
{
    public static final AtomicInteger TICKER = new AtomicInteger();

    @State(Scope.Benchmark)
    public static class MyState
    {
        private volatile int tickSetInstance;
        private volatile int tickSetIteration;
        private volatile int tickSetInvocation;
        private volatile int tickTearInstance;
        private volatile int tickTearIteration;
        private volatile int tickTearInvocation;
        private volatile int tickRun;

        @Setup(Level.Trial)
        public void setupInstance()
        {
            tickSetInstance = TICKER.incrementAndGet();
        }

        @Setup(Level.Iteration)
        public void setupIteration()
        {
            tickSetIteration = TICKER.incrementAndGet();
        }

        @Setup(Level.Invocation)
        public void setupInvocation()
        {
            tickSetInvocation = TICKER.incrementAndGet();
        }

        @TearDown(Level.Invocation)
        public void tearDownInvocation()
        {
            tickTearInvocation = TICKER.incrementAndGet();
        }

        @TearDown(Level.Iteration)
        public void tearDownIteration()
        {
            tickTearIteration = TICKER.incrementAndGet();
        }

        @TearDown(Level.Trial)
        public void tearDownInstance()
        {
            tickTearInstance = TICKER.incrementAndGet();

            Assert.assertTrue("Setup/instance called before setup/iteration", tickSetInstance < tickSetIteration);
            Assert.assertTrue("Setup/iteration called before setup/invocation", tickSetIteration < tickSetInvocation);
            Assert.assertTrue("Setup/invocation called before run", tickSetInvocation < tickRun);
            Assert.assertTrue("Run called before tear/invocation", tickRun < tickTearInvocation);
            Assert.assertTrue("Tear/invocation called before tear/iteration", tickTearInvocation < tickTearIteration);
            Assert.assertTrue("Tear/iteration called before tear/instance", tickTearIteration < tickTearInstance);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Fork(1)
    @Threads(1)
    public void test(BenchmarkStateOrderIB.MyState state)
    {
        state.tickRun = TICKER.incrementAndGet();
        Work.work();
    }
}
