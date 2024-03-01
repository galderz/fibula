package org.mendrugo.fibula.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public enum Vm
{
    HOTSPOT
    , SUBSTRATE;

    private static final File RUN_JAR = Path.of("target/runner-jvm/quarkus-run.jar").toFile();
    private static final File RUN_BINARY = findRunBinary();

    private static File findRunBinary()
    {
        final Path runnerNative = Path.of("target/runner-native");
        if (!runnerNative.toFile().exists())
        {
            return new File("NOT_FOUND");
        }

        try (Stream<Path> walk = Files.walk(runnerNative)) {
            return walk
                .filter(p -> !Files.isDirectory(p))
                .map(Path::toString)
                .filter(f -> f.endsWith("-runner"))
                .findFirst()
                .map(File::new)
                .orElse(new File("NOT_FOUND"));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

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
