package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.BenchmarkTaskResult;

import java.util.Collection;
import java.util.List;

public record NativeBenchmarkTaskResult(
    long allOperations
    , long measuredOperations
    , Collection<NativeThroughputResult> results
)
{
    public static NativeBenchmarkTaskResult of(BenchmarkTaskResult obj)
    {
        final List<NativeThroughputResult> results = obj.getResults().stream().map(NativeResult::of).toList();
        return new NativeBenchmarkTaskResult(
            obj.getAllOps()
            , obj.getMeasuredOps()
            , results
        );
    }
}
