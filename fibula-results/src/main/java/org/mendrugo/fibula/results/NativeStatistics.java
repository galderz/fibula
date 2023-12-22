package org.mendrugo.fibula.results;

import org.openjdk.jmh.util.SingletonStatistics;
import org.openjdk.jmh.util.Statistics;

public interface NativeStatistics
{
    static NativeSingletonStatistics of(Statistics statistics)
    {
        if (statistics instanceof SingletonStatistics ss)
        {
            return new NativeSingletonStatistics(ss.getSum());
        }
        return null;  // TODO: Customise this generated block
    }
}
