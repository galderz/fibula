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
    HOTSPOT, NATIVE_RUN, NATIVE_TESTS;

    private static final File NOT_FOUND = new File("NOT_FOUND");
    private static final File RUN_JAR = Paths.get("target/benchmarks.jar").toFile();
    private static final File RUN_BINARY = findBinary("benchmarks");
    private static final File TEST_BINARY = findBinary("native-tests");

    private static File findBinary(String binaryName)
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
                .filter(f -> f.endsWith(binaryName))
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

            return NATIVE_RUN;
        }

        if (RUN_JAR.exists())
        {
            return HOTSPOT;
        }

        if (RUN_BINARY.exists())
        {
            return NATIVE_RUN;
        }

        if (TEST_BINARY.exists())
        {
            return NATIVE_TESTS;
        }

        return HOTSPOT;
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
            case NATIVE_RUN:
                return new Info(
                    binaryReadString("com.oracle.svm.core.VM.Java.Version=", RUN_BINARY)
                    , "Substrate VM"
                    , binaryReadString("com.oracle.svm.core.VM=", RUN_BINARY)
                );
            case NATIVE_TESTS:
                return new Info(
                    binaryReadString("com.oracle.svm.core.VM.Java.Version=", TEST_BINARY)
                    , "Substrate VM"
                    , binaryReadString("com.oracle.svm.core.VM=", TEST_BINARY)
                );
            default:
                throw new IllegalStateException("Unknown value " + this);
        }
    }

    private String binaryReadString(String key, File binary)
    {
        // todo support windows
        final List<String> args = Arrays.asList(
            "/bin/sh"
            , "-c"
            , String.format(
                "strings %s| grep %s"
                , binary.getPath()
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

    String executablePath(String jvm)
    {
        switch (this)
        {
            case HOTSPOT:
                return new File(jvm).getPath();
            case NATIVE_RUN:
                return RUN_BINARY.getPath();
            case NATIVE_TESTS:
                return TEST_BINARY.getPath();
            default:
                throw new IllegalStateException("Unknown value " + this);
        }
    }

    boolean isNativeVm()
    {
        return this == NATIVE_RUN;
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
