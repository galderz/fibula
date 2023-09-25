//package org.mendrugo.fibula.runner.sample;
//
//import org.mendrugo.fibula.results.ThroughputResult;
//import org.mendrugo.fibula.runner.Infrastructure;
//
//import java.util.concurrent.Callable;
//
//// todo generate automatically
//public class SampleBenchmark implements Callable<ThroughputResult>
//{
//    private final Infrastructure infra;
//
//    public SampleBenchmark(Infrastructure infra)
//    {
//        this.infra = infra;
//    }
//
//    @Override
//    public ThroughputResult call() throws Exception
//    {
//        long operations = 0;
//        long startTime = System.nanoTime();
//        do
//        {
//            sampleMethod();
//            operations++;
//        }
//        while(!infra.isDone);
//        long stopTime = System.nanoTime();
//        return ThroughputResult.of("blah", operations, stopTime, startTime);
//    }
//
//    private void sampleMethod() {}
//}
