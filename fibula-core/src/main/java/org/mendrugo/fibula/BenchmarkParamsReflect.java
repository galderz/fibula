package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

final class BenchmarkParamsReflect
{
    private final BenchmarkParams benchmarkParams;
    private final OutputFormat out;

    BenchmarkParamsReflect(BenchmarkParams benchmarkParams, OutputFormat out) {
        this.benchmarkParams = benchmarkParams;
        this.out = out;
    }

    void amendField(String fieldName, Object newValue)
    {
        try
        {
            final Class<?> clazz = Class.forName("org.openjdk.jmh.infra.BenchmarkParamsL2");
            final Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(benchmarkParams, newValue);
        }
        catch (Exception e)
        {
            out.println(String.format("Unable to amend benchmark params field %s", fieldName));
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            out.verbosePrintln(stringWriter.toString());
        }
    }
}
