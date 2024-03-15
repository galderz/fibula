package org.mendrugo.fibula.samples.it;

import org.openjdk.jmh.runner.Defaults;

import java.nio.file.Path;

record Parameters(
    String benchmarkName
    , int timeoutMins
    , int measurementForkCount
    , int measurementIterationCount
    , String measurementTime
    , int warmupIterationCount
    , String warmupTime
)
{
    static final boolean QUICK = Boolean.getBoolean("fibula.test.quick");

    static final int QUICK_TIMEOUT = 1;
    static final String QUICK_MEASUREMENT_TIME = "1 s";
    static final int QUICK_MEASUREMENT_FORKS = 1;
    static final int QUICK_MEASUREMENT_ITERATIONS = 1;
    static final String QUICK_WARMUP_TIME = "1 s";
    static final int QUICK_WARMUP_ITERATIONS = 1;

    static Parameters parameters(String benchmarkName)
    {
        if (QUICK)
        {
            return new Parameters(
                benchmarkName
                , QUICK_TIMEOUT
                , QUICK_MEASUREMENT_FORKS
                , QUICK_MEASUREMENT_ITERATIONS
                , QUICK_MEASUREMENT_TIME
                , QUICK_WARMUP_ITERATIONS
                , QUICK_WARMUP_TIME
            );
        }

        return new Parameters(
            benchmarkName
            , 120 // minutes
            , Defaults.MEASUREMENT_FORKS
            , Defaults.MEASUREMENT_ITERATIONS
            , Defaults.MEASUREMENT_TIME.toString()
            , Defaults.WARMUP_ITERATIONS
            , Defaults.WARMUP_TIME.toString()
        );
    }

    Path resultPath()
    {
        return Path.of(
            "target"
            , String.format(
                "fibula_%s.json"
                , benchmarkName
            )
        );
    }
}
