package org.mendrugo.fibula;

import java.lang.reflect.Method;

public class SwitchingForkedMain
{
    public static void main(String[] args) throws Exception
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
}
