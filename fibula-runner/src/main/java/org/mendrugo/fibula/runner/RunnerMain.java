package org.mendrugo.fibula.runner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import java.lang.reflect.Method;

@QuarkusMain(name = "runner")
public class RunnerMain implements QuarkusApplication
{
    @Override
    public int run(String... args) throws Exception
    {
        final Cli cli = Cli.read(args);
        final String host = cli.text("host");
        final String port = cli.text("port");
        invokeForkedMain(host, port);
        return 0;
    }

    private static void invokeForkedMain(String host, String port)
    {
        try
        {
            final Class<?> clazz = Class.forName("org.openjdk.jmh.runner.ForkedMain");
            final Method mainMethod = clazz.getDeclaredMethod("main", String[].class);
            mainMethod.setAccessible(true);
            mainMethod.invoke(null, new Object[]{new String[]{host, port}});
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
