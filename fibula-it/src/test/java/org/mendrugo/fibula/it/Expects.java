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

final class Expects
{
    static void expectScoresNearEqual(String benchmarkName)
    {
        Expects.assertScores(
            Benchmark.run(benchmarkName, Provider.FIBULA)
            , Benchmark.run(benchmarkName, Provider.JMH)
        );
    }

    private static void assertScores(List<Result> fibulaResults, List<Result> jmhResults)
    {
        System.out.println(fibulaResults);
        System.out.println(jmhResults);

        assertThat(fibulaResults.size(), is(jmhResults.size()));
        for (int i = 0; i < fibulaResults.size(); i++)
        {
            final Result fibulaResult = fibulaResults.get(i);
            final Result jmhResult = jmhResults.get(i);
            assertThat(fibulaResult.benchmark(), is(equalTo(jmhResult.benchmark())));
            double relativeTolerance = 0.05;
            assertThat(
                fibulaResult.primaryMetric().score()
                , is(closeTo(
                    jmhResult.primaryMetric().score()
                    , jmhResult.primaryMetric().score() * relativeTolerance
                )));
        }
    }

    static List<Result> assertSanityChecks(Parameters test, Provider provider)
    {
        final List<Result> results = toResults(provider.workingDir().resolve(test.resultPath()));
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

    private Expects()
    {
    }
}
