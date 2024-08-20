package org.mendrugo.fibula.bootstrap;

import joptsimple.internal.Strings;
import org.mendrugo.fibula.bootstrap.ProcessExecutor.ProcessResult;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    public String executablePath(String jvm)
    {
        return switch (this)
        {
            case HOTSPOT -> new File(jvm).getPath();
            case SUBSTRATE -> RUN_BINARY.getPath();
        };
    }

    public Collection<String> jvmArgs(Collection<String> jvmArgs)
    {
        return switch (this)
        {
            case HOTSPOT -> jvmArgs;
            case SUBSTRATE -> jvmArgs.stream()
                .filter(arg -> !skipNativeInvalidJvmArgs().matcher(arg).matches())
                .collect(Collectors.toCollection(ArrayList::new));
        };
    }

    public List<String> vmArguments(String jvm, Collection<String> jvmArgs, List<String> javaOptions)
    {
        return switch (this)
        {
            case HOTSPOT -> getHotSpotArguments(jvm, jvmArgs, javaOptions);
            case SUBSTRATE -> getNativeArguments(jvmArgs, javaOptions);
        };
    }

    public Info info(OutputFormat out)
    {
        return switch (this)
        {
            case HOTSPOT -> new Info(
                System.getProperty("java.version")
                , System.getProperty("java.vm.name")
                , System.getProperty("java.vm.version")
            );
            case SUBSTRATE -> new Info(
                binaryReadString("com.oracle.svm.core.VM.Java.Version=", out)
                , "Substrate VM"
                , binaryReadString("com.oracle.svm.core.VM=", out)
            );
        };
    }

    // todo support windows
    private String binaryReadString(String key, OutputFormat out)
    {
        final ProcessExecutor processExecutor = new ProcessExecutor(out);
        final List<String> args = List.of(
            "/bin/sh"
            , "-c"
            , String.format(
                "strings %s| grep %s"
                , RUN_BINARY.getPath()
                , key
            )
        );
        try (final ProcessResult result = processExecutor.runSync(args, false, false))
        {
            final String output = Files.readString(
                Path.of(result.stdOut().getAbsolutePath())
                , StandardCharsets.UTF_8
            );
            return output
                .split("=")[1] // extract only the version
                .trim(); // trim to remove any additional space or carriage return
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static Vm instance()
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

        if (Boolean.getBoolean("fibula.runner.debug"))
        {
            args.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:9000");
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

    private static Pattern skipNativeInvalidJvmArgs()
    {
        final List<String> skipJvmArgs = List.of(
            "-XX:(\\+|-)UnlockExperimentalVMOptions"
            , "-XX:(\\+|-)EnableJVMCIProduct"
            , "-XX:ThreadPriorityPolicy=\\d+"
        );

        return Pattern.compile(Strings.join(skipJvmArgs, "|"));
    }

    public record Info(
        String jdkVersion
        , String vmName
        , String vmVersion
    ) {}
}
