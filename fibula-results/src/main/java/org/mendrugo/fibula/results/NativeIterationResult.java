package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;

import java.util.ArrayList;
import java.util.Collection;

// 1 per iteration
public record NativeIterationResult(
    long allOperations
    , long measuredOperations
    , Collection<NativeResult> primaryResults
)
{
    public static NativeIterationResult of(IterationResult iterationResult)
    {
        Collection<Result> primaryResults = new ArrayList<>(iterationResult.getRawPrimaryResults());
        return new NativeIterationResult(
            iterationResult.getMetadata().getAllOps()
            , iterationResult.getMetadata().getMeasuredOps()
            , primaryResults.stream().map(NativeResult::of).toList()
        );
    }
}
