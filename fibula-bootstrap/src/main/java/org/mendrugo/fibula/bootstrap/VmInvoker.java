package org.mendrugo.fibula.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public enum VmInvoker
{

    JVM
    , NATIVE;

    private static final File JVM_JAR = new File("target/runner-jvm/quarkus-run.jar");
    private static final File NATIVE_RUNNER = new File("target/runner-native/fibula-samples-1.0.0-SNAPSHOT-runner");

    String vm(String jvm)
    {
        return switch (this)
        {
            case JVM -> new File(jvm).getPath();
            case NATIVE -> NATIVE_RUNNER.getPath();
        };
    }

    List<String> vmArguments(String jvm)
    {
        return switch (this)
        {
            case JVM -> getJvmArguments(jvm);
            case NATIVE -> List.of(NATIVE_RUNNER.getPath());
        };
    }

    static VmInvoker get()
    {
        if (JVM_JAR.exists() && NATIVE_RUNNER.exists())
        {
            if (JVM_JAR.lastModified() > NATIVE_RUNNER.lastModified())
            {
                return VmInvoker.JVM;
            }

            return VmInvoker.NATIVE;
        }

        if (JVM_JAR.exists())
        {
            return VmInvoker.JVM;
        }

        if (NATIVE_RUNNER.exists())
        {
            return VmInvoker.NATIVE;
        }

        throw new IllegalStateException("Could not resolve which VM invoker to use");
    }

    private static List<String> getJvmArguments(String jvm)
    {
        final List<String> args = new ArrayList<>();
        args.add(jvm);

        final String logLevelPropertyName = "quarkus.log.category.\"org.mendrugo.fibula\".level";
        final String logLevel = System.getProperty(logLevelPropertyName);
        if (logLevel != null)
        {
            args.add(String.format("-D%s=%s", logLevelPropertyName, logLevel));
        }

        // todo add an option for the native image agent and fix location of java
        // , "-agentlib:native-image-agent=config-output-dir=target/native-agent-config"

        args.add("-jar");
        args.add(VmInvoker.JVM_JAR.getPath());

        return args;
    }
}
