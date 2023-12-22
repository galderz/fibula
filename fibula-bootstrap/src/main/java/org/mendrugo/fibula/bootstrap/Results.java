package org.mendrugo.fibula.bootstrap;

import org.mendrugo.fibula.results.NativeResult;
import org.mendrugo.fibula.results.NativeSingletonStatistics;
import org.mendrugo.fibula.results.NativeStatistics;
import org.mendrugo.fibula.results.NativeThroughputResult;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.util.SingletonStatistics;
import org.openjdk.jmh.util.Statistics;

import java.lang.reflect.Constructor;

final class Results
{
    static Result toResult(NativeResult result)
    {
        if (result instanceof NativeThroughputResult throughputResult)
        {
            return toResult(throughputResult);
        }
        return null;
    }

    static Result toResult(NativeThroughputResult result)
    {
        try
        {
            final Constructor<ThroughputResult> ctor = ThroughputResult.class.getDeclaredConstructor(
                ResultRole.class
                , String.class
                , Statistics.class
                , String.class
                , AggregationPolicy.class
            );

            // Get around package private constructor
            // todo ask shipilev if it could be made public
            ctor.setAccessible(true);

            return ctor.newInstance(
                result.role()
                , result.label()
                , toStatistics(result.statistics())
                , result.unit()
                , result.policy()
            );
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    static Statistics toStatistics(NativeStatistics statistics)
    {
        if (statistics instanceof NativeSingletonStatistics singletonStatistics)
        {
            return new SingletonStatistics(singletonStatistics.value());
        }
        return null;
    }

    private Results() {}
}
