package org.mendrugo.fibula.bootstrap;

import joptsimple.internal.Strings;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum Vm
{
    HOTSPOT
    , SUBSTRATE;

    private static final File RUN_JAR = Path.of("target/quarkus-app/quarkus-run.jar").toFile();
    private static final File RUN_BINARY = findRunBinary();

    private static File findRunBinary()
    {
        final Path targetDir = Path.of("target");
        if (!targetDir.toFile().exists())
        {
            return new File("NOT_FOUND");
        }

        try (Stream<Path> walk = Files.walk(targetDir)) {
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

    Collection<String> jvmArgs(Collection<String> jvmArgs)
    {
        return switch (this)
        {
            case HOTSPOT -> jvmArgs;
            case SUBSTRATE -> jvmArgs.stream()
                .filter(arg -> !skipNativeJvmArgs().matcher(arg).matches())
                .toList();
        };
    }

    List<String> vmArguments(String jvm, Collection<String> jvmArgs, List<String> javaOptions)
    {
        return switch (this)
        {
            case HOTSPOT -> getHotSpotArguments(jvm, jvmArgs, javaOptions);
            case SUBSTRATE -> getNativeArguments(jvmArgs, javaOptions);
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

    private static List<String> getHotSpotArguments(String jvm, Collection<String> jvmArgs, List<String> javaOptions)
    {
        final List<String> args = new ArrayList<>();
        args.add(jvm);
        args.addAll(jvmArgs);

        final String logLevelPropertyName = "quarkus.log.category.\"org.mendrugo.fibula\".level";
        final String logLevel = System.getProperty(logLevelPropertyName);
        if (logLevel != null)
        {
            args.add(String.format("-D%s=%s", logLevelPropertyName, logLevel));
        }

        if (Boolean.getBoolean("fibula.native.agent"))
        {
            args.add("-agentlib:native-image-agent=config-output-dir=target/native-agent-config");
        }

        args.addAll(javaOptions);

        args.add("-jar");
        args.add(Vm.RUN_JAR.getPath());

        return args;
    }

    private static List<String> getNativeArguments(Collection<String> jvmArgs, List<String> javaOptions)
    {
        final List<String> args = new ArrayList<>();
        args.add(RUN_BINARY.getPath());
        args.addAll(jvmArgs);
        args.addAll(javaOptions);
        return args;
    }

    private static Pattern skipNativeJvmArgs()
    {
        final List<String> skipJvmArgs = List.of(
            "-XX:(\\+|-)UnlockExperimentalVMOptions"
            , "-XX:(\\+|-)EnableJVMCIProduct"
            , "-XX:ThreadPriorityPolicy=\\d+"
        );

        return Pattern.compile(Strings.join(skipJvmArgs, "|"));
    }
}
