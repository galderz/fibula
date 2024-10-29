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

            String[] invokeArgs;
            // TODO this check might not be needed because this class only runs when native is enabled,
            //      otherwise ForkedMain is run directly
            if (isNativeRun())
            {
                // If more than 2 arguments passed in, keep only the last two
                invokeArgs = new String[2];
                invokeArgs[0] = args[args.length - 2];
                invokeArgs[1] = args[args.length - 1];
            }
            else
            {
                invokeArgs = args;
            }
            
            mainMethod.invoke(null, new Object[]{invokeArgs});
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static boolean isNativeRun()
    {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    @Override
    public int run(String... args) throws Exception
    {
        // No-op
        return 0;
    }
}
