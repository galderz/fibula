package org.mendrugo.fibula.results;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ThroughputResult;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value=NativeThroughputResult.class, name = "Throughput"),
})
public interface NativeResult
{
    static NativeResult of(Result result)
    {
        if (result instanceof ThroughputResult tr)
        {
            return NativeThroughputResult.of(tr);
        }
        return null;  // TODO: Customise this generated block
    }
}
