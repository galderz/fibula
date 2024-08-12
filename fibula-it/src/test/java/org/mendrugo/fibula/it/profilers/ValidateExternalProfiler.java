package org.mendrugo.fibula.it.profilers;

import org.openjdk.jmh.it.profilers.ItExternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

import java.io.File;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ValidateExternalProfiler extends ItExternalProfiler
{
    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr)
    {
        assertThat(br.getMetadata(), is(notNullValue()));
        return super.afterTrial(br, pid, stdOut, stdErr);
    }
}
