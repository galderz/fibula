package org.mendrugo.fibula.results;

import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.options.TimeValue;

public record NativeIterationParams(
    IterationType type
    , int count
//    , TimeValue timeValue
//    , int batchSize
)
{
}
