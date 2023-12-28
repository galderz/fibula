package org.mendrugo.fibula.results;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.Defaults;

import java.util.Optional;

public final class NativeBenchmarkParams
{
    private final BenchmarkListEntry benchmark;
    private final String annotationParams;

    public NativeBenchmarkParams(String annotationParams)
    {
        this.annotationParams = annotationParams;
        this.benchmark = new BenchmarkListEntry(annotationParams);
    }

    public int getMeasurementForks(Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getForks()
            .orElse(Defaults.MEASUREMENT_FORKS));
    }

    public int getMeasurementIterations(Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getMeasurementIterations()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT
                : Defaults.MEASUREMENT_ITERATIONS)
            );
    }

    public int getWarmupIterations(Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getWarmupIterations()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? Defaults.WARMUP_ITERATIONS_SINGLESHOT
                : Defaults.WARMUP_ITERATIONS)
            );
    }

    public String getAnnotationParams()
    {
        return annotationParams;
    }
}

//public record NativeBenchmarkParams(
//    String benchmark
////    , boolean synchIterations
////    , int threads
////    , int[] threadGroups
////    , Collection<String> threadGroupLabels
//    , int forks
//    , int warmupForks
//    , NativeIterationParams warmup
//    , NativeIterationParams measurement
//    , Mode mode
////    , WorkloadParams params
////    , TimeUnit timeUnit
////    , int opsPerInvocation
////    , String jvm
////    , Collection<String> jvmArgs
////    , String jdkVersion
////    , String jmhVersion
////    , String vmName
////    , String vmVersion
////    , TimeValue timeout
//)
//{
//}
