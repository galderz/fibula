package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.util.SingletonStatistics;

public record NativeThroughputResult(
    ResultRole role
    , String label
    , NativeSingletonStatistics statistics
    , String unit
    , AggregationPolicy policy
) implements NativeResult
{
    static NativeThroughputResult of(ThroughputResult obj)
    {
        return new NativeThroughputResult(
            obj.getRole()
            , obj.getLabel()
            , new NativeSingletonStatistics(obj.getStatistics().getSum())
            , obj.getScoreUnit()
            , AggregationPolicy.SUM // fixed because there's no getter
        );
    }
}
