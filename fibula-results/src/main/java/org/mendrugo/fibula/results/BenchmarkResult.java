package org.mendrugo.fibula.results;

import java.util.List;

public record BenchmarkResult(
    List<IterationResult> iterationResults
)
{
    ThroughputResult aggregate()
    {
        final double sum = sum(iterationResults);
        final double average = average(sum, iterationResults.size());
        return null;
    }

    double sum(List<IterationResult> iterationResults)
    {
        double sum = 0;
        for (IterationResult iterationResult : iterationResults)
        {
            sum += iterationResult.result().statistic();
        }
        return sum;
    }

    double average(double sum, int count)
    {
        return sum / count;
    }
}
