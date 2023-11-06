package org.mendrugo.fibula.results;

import java.util.concurrent.TimeUnit;

public record ThroughputResult(
    String label
    , double operations
    , double statistic
    , TimeUnit unit
)
{
//    public static ThroughputResult of(
//        String label
//        , double operations
//        , long stopTime
//        , long startTime
//    )
//    {
//        long duration = stopTime - startTime;
//        TimeUnit outputTimeUnit = TimeUnit.SECONDS;
//        double statistic = operations * TimeUnit.NANOSECONDS.convert(1, outputTimeUnit) / duration;
//        return new ThroughputResult(label, operations, statistic, "ops/s");
//    }
}
