package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.BenchmarkTaskResult;

import java.util.Collection;
import java.util.List;

// 1 per iteration per thread
public record NativeBenchmarkTaskResult(
    long allOperations
    , long measuredOperations
    , Collection<NativeResult> results
)
{
    public static NativeBenchmarkTaskResult of(BenchmarkTaskResult obj)
    {
        final List<NativeResult> results = obj.getResults().stream().map(NativeResult::of).toList();
        return new NativeBenchmarkTaskResult(
            obj.getAllOps()
            , obj.getMeasuredOps()
            , results
        );
    }
}
