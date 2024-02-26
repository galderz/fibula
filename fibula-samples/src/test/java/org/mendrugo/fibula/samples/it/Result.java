package org.mendrugo.fibula.samples.it;

import java.util.List;

public record Result(
    String benchmark
    , int forks
    , int warmupIterations
    , String warmupTime
    , int measurementIterations
    , String measurementTime
    , Metric primaryMetric
)
{
    public record Metric(
        double score
        , String scoreUnit
        , List<List<Double>> rawData
    )
    {
        int rawDataSize()
        {
            return rawData.isEmpty() ? 0 : rawData.size() * rawData.getFirst().size();
        }
    }
}
