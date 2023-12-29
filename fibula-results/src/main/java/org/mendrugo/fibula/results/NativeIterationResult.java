package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ThroughputResult;

import java.util.ArrayList;
import java.util.Collection;

// 1 per iteration
public record NativeIterationResult(
    long allOperations
    , long measuredOperations
    , Collection<NativeThroughputResult> primaryResults
    , String annotationParams
)
{
    public static NativeIterationResult of(IterationResult iterationResult, String annotationParams)
    {
        Collection<Result> primaryResults = new ArrayList<>(iterationResult.getRawPrimaryResults());
        return new NativeIterationResult(
            iterationResult.getMetadata().getAllOps()
            , iterationResult.getMetadata().getMeasuredOps()
            , primaryResults.stream()
                .map(result -> (ThroughputResult) result)
                .map(NativeThroughputResult::of)
                .toList()
            , annotationParams
        );
    }
}
