package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;

public record NativeThroughputResult(
    ResultRole role
    , String label
    , NativeStatistics statistics
    , String unit
    , AggregationPolicy policy
) implements NativeResult
{
    static NativeThroughputResult of(ThroughputResult obj)
    {
        return new NativeThroughputResult(
            obj.getRole()
            , obj.getLabel()
            , NativeStatistics.of(obj.getStatistics())
            , obj.getScoreUnit()
            , AggregationPolicy.SUM // fixed because there's no getter
        );
    }
}
