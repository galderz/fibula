package org.mendrugo.fibula.it;

import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;

import java.util.Arrays;
import java.util.Map;

public final class SecondaryResults
{
    public static Result reduce(Map<String, Result> secondaryResults, String... names)
    {
        for (String name : names) {
            Result r = secondaryResults.get(name);
            if (r != null) {
                return r;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String k : secondaryResults.keySet()) {
            sb.append(k);
            sb.append(" = ");
            sb.append(secondaryResults.get(k));
            sb.append(System.lineSeparator());
        }
        throw new IllegalStateException("Cannot find the result for " + Arrays.toString(names) + "\". Available entries: " + sb);
    }
    
    private SecondaryResults() {}
}
