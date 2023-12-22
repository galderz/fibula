package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ThroughputResult;

public interface NativeResult
{
    static NativeThroughputResult of(Result result)
    {
        if (result instanceof ThroughputResult tr)
        {
            return NativeThroughputResult.of(tr);
        }
        return null;  // TODO: Customise this generated block
    }
}
