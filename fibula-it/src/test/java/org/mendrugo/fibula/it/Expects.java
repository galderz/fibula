package org.mendrugo.fibula.it;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Expects
{
    static List<Result> assertSanityChecks(TestParameters test)
    {
        final List<Result> results = toResults(test.resultReadPath());
        for (Result result : results)
        {
            assertThat(result.forks(), is(test.measurementForkCount()));
            assertThat(result.measurementIterations(), is(test.measurementIterationCount()));
            assertThat(result.measurementTime(), is(test.measurementTime()));
            assertThat(result.warmupIterations(), is(test.warmupIterationCount()));
            assertThat(result.warmupTime(), is(test.warmupTime()));
            assertThat(result.primaryMetric().score(), is(greaterThan(0d)));
            assertThat(result.primaryMetric().score(), is(greaterThan(0d)));
            assertThat(result.primaryMetric().rawDataSize(), is(test.measurementForkCount() * test.measurementIterationCount()));
        }
        return results;
    }

    private static List<Result> toResults(Path resultPath)
    {
        final String json = toJsonString(resultPath);
        final Type type = Types.newParameterizedType(List.class, Result.class);
        final Moshi moshi = new Moshi.Builder().build();
        final JsonAdapter<List<Result>> adapter = moshi.adapter(type);
        try
        {
            return adapter.fromJson(json);
        }
        catch (IOException e)
        {
            throw new AssertionError(e);
        }
    }

    private static String toJsonString(Path resultPath)
    {
        try
        {
            return Files.readString(resultPath);
        }
        catch (IOException e)
        {
            throw new AssertionError(e);
        }
    }
}
