package org.mendrugo.fibula.bootstrap;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public enum Vm
{
    HOTSPOT
    , SUBSTRATE;

    // todo make it more CDI idiomatic
    private static final Path ROOT = Path.of(System.getProperty("fibula.root", "."));

    private static final File RUN_JAR = ROOT.resolve(Path.of("target/runner-jvm/quarkus-run.jar")).toFile();
    private static final File RUN_BINARY = ROOT.resolve(Path.of("target/runner-native/fibula-samples-1.0.0-SNAPSHOT-runner")).toFile();

    String executablePath(String jvm)
    {
        return switch (this)
        {
            case HOTSPOT -> new File(jvm).getPath();
            case SUBSTRATE -> RUN_BINARY.getPath();
        };
    }

    List<String> vmArguments(String jvm)
    {
        return switch (this)
        {
            case HOTSPOT -> getHotSpotArguments(jvm);
            case SUBSTRATE -> List.of(RUN_BINARY.getPath());
        };
    }

    static Vm instance()
    {
        if (RUN_JAR.exists() && RUN_BINARY.exists())
        {
            if (RUN_JAR.lastModified() > RUN_BINARY.lastModified())
            {
                return Vm.HOTSPOT;
            }

            return Vm.SUBSTRATE;
        }

        if (RUN_JAR.exists())
        {
            return Vm.HOTSPOT;
        }

        if (RUN_BINARY.exists())
        {
            return Vm.SUBSTRATE;
        }

        throw new IllegalStateException("Could not resolve which VM invoker to use");
    }

    private static List<String> getHotSpotArguments(String jvm)
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
        args.add(Vm.RUN_JAR.getPath());

        return args;
    }
}
