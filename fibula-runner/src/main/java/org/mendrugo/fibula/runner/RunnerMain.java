package org.mendrugo.fibula.runner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.lang.reflect.Method;

@QuarkusMain
public class RunnerMain implements QuarkusApplication
{
    public static void main(String... args)
    {
        try
        {
            final Class<?> clazz = Class.forName("org.openjdk.jmh.runner.ForkedMain");

            final Method mainMethod = clazz.getDeclaredMethod("main", String[].class);
            mainMethod.setAccessible(true);

            // The arguments are built by JMH and it assumes it invokes a Java application,
            // but this runner only gets invoked for native executions,
            // so only the last 2 arguments are relevant,
            // the rest can be ignored.
            final String[] invokeArgs = new String[2];
            invokeArgs[0] = args[args.length - 2];
            invokeArgs[1] = args[args.length - 1];

            mainMethod.invoke(null, new Object[]{invokeArgs});
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
