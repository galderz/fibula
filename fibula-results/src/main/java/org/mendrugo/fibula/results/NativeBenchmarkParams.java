package org.mendrugo.fibula.results;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Optional;

public final class NativeBenchmarkParams
{
    private final BenchmarkListEntry benchmark;

    public NativeBenchmarkParams(BenchmarkListEntry benchmark)
    {
        this.benchmark = benchmark;
    }

    public String getBenchmark()
    {
        return benchmark.getUsername();
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

    public TimeValue getMeasurementTime(Optional<TimeValue> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getMeasurementTime()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? TimeValue.NONE
                : Defaults.MEASUREMENT_TIME)
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

    public TimeValue getWarmupTime(Optional<TimeValue> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getWarmupTime()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? TimeValue.NONE
                : Defaults.WARMUP_TIME)
            );
    }
}
