package org.mendrugo.fibula.runner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.lang.reflect.Method;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    public static void main(String... args)
    {
        try
        {
            final Class<?> clazz = Class.forName("org.openjdk.jmh.runner.ForkedMain");
            final Method mainMethod = clazz.getDeclaredMethod("main", String[].class);
            mainMethod.setAccessible(true);
            mainMethod.invoke(null, new Object[]{args});
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int run(String... args) throws Exception
    {
        // No-op
        return 0;
    }
}
