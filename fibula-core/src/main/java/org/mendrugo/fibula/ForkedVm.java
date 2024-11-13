package org.mendrugo.fibula;

import org.openjdk.jmh.runner.BenchmarkException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

enum ForkedVm
{
    HOTSPOT, SUBSTRATE;

    private static final File NOT_FOUND = new File("NOT_FOUND");
    private static final File RUN_JAR = Paths.get("target/benchmarks.jar").toFile();
    private static final File RUN_BINARY = findRunBinary();

    private static File findRunBinary()
    {
        final Path targetDir = Paths.get("target");
        if (!targetDir.toFile().exists())
        {
            return NOT_FOUND;
        }

        try (Stream<Path> walk = Files.walk(targetDir))
        {
            return walk
                .filter(p -> !Files.isDirectory(p))
                .map(Path::toString)
                .filter(f -> f.endsWith("benchmarks"))
                .findFirst()
                .map(File::new)
                .orElse(NOT_FOUND);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    static ForkedVm instance()
    {
        if (RUN_JAR.exists() && RUN_BINARY.exists())
        {
            if (RUN_JAR.lastModified() > RUN_BINARY.lastModified())
            {
                return HOTSPOT;
            }

            return SUBSTRATE;
        }

        if (RUN_JAR.exists())
        {
            return HOTSPOT;
        }

        if (RUN_BINARY.exists())
        {
            return SUBSTRATE;
        }

        throw new IllegalStateException("Could not resolve which VM invoker to use");
    }

    public Info info()
    {
        switch (this)
        {
            case HOTSPOT:
                return new Info(
                    System.getProperty("java.version")
                    , System.getProperty("java.vm.name")
                    , System.getProperty("java.vm.version")
                );
            case SUBSTRATE:
                return new Info(
                    binaryReadString("com.oracle.svm.core.VM.Java.Version=")
                    , "Substrate VM"
                    , binaryReadString("com.oracle.svm.core.VM=")
                );
            default:
                throw new IllegalStateException("Unknown value " + this);
        }
    }

    private String binaryReadString(String key)
    {
        // todo support windows
        final List<String> args = Arrays.asList(
            "/bin/sh"
            , "-c"
            , String.format(
                "strings %s| grep %s"
                , RUN_BINARY.getPath()
                , key
            )
        );

        final ProcessBuilder processBuilder = new ProcessBuilder(args);
        try
        {
            final Process process = processBuilder.start();
            final BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
            final int exitValue = process.waitFor();
            if (exitValue == 0)
            {
                final String line = output.readLine();
                return line
                    .split("=")[1] // extract only the version
                    .trim(); // trim to remove any additional space or carriage return
            }
            throw new BenchmarkException(
                new IllegalStateException(
                    String.format(
                        "Reading strings from binary with %s failed with exit code %d"
                        , args
                        , exitValue
                    )
                )
            );
        }
        catch (IOException | InterruptedException e)
        {
            throw new BenchmarkException(e);
        }
    }

    public String executablePath(String jvm)
    {
        switch (this)
        {
            case HOTSPOT:
                return new File(jvm).getPath();
            case SUBSTRATE:
                return RUN_BINARY.getPath();
            default:
                throw new IllegalStateException("Unknown value " + this);
        }
    }

    static class Info
    {
        final String jdkVersion;
        final String vmName;
        final String vmVersion;

        Info(String jdkVersion, String vmName, String vmVersion)
        {
            this.jdkVersion = jdkVersion;
            this.vmName = vmName;
            this.vmVersion = vmVersion;
        }
    }
}
