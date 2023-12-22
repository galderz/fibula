package org.mendrugo.fibula.results;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openjdk.jmh.util.SingletonStatistics;
import org.openjdk.jmh.util.Statistics;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value=NativeSingletonStatistics.class, name = "Singleton"),
})
public interface NativeStatistics
{
    static NativeStatistics of(Statistics statistics)
    {
        if (statistics instanceof SingletonStatistics ss)
        {
            return new NativeSingletonStatistics(ss.getSum());
        }
        return null;  // TODO: Customise this generated block
    }
}
