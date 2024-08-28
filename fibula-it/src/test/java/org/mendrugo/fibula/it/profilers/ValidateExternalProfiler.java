package org.mendrugo.fibula.it.profilers;

import org.junit.Assert;
import org.openjdk.jmh.it.profilers.ItExternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;

import java.io.File;
import java.util.Collection;

public class ValidateExternalProfiler extends ItExternalProfiler
{
    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr)
    {
        Assert.assertNotNull(br.getMetadata());
        return super.afterTrial(br, pid, stdOut, stdErr);
    }
}
