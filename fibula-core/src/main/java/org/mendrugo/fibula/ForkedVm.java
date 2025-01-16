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

record ForkedVm(
    String jdkVersion
    , String vmName
    , String vmVersion
    , File executable
)
{
    private static final File NOT_FOUND = new File("NOT_FOUND");
    private static final File RUN_JAR = Path.of("target", "benchmarks.jar").toFile();

    static ForkedVm instance()
    {
        final File runBinary = findRunBinary();
        if (isNativeVm(runBinary))
        {
            return new ForkedVm(
                binaryReadString("com.oracle.svm.core.VM.Java.Version=", runBinary)
                , "Substrate VM"
                , binaryReadString("com.oracle.svm.core.VM=", runBinary)
                , runBinary
            );
        }

        return new ForkedVm(
            System.getProperty("java.version")
            , System.getProperty("java.vm.name")
            , System.getProperty("java.vm.version")
            , RUN_JAR
        );
    }

    String executablePath(String jvm)
    {
        if (isNativeVm())
        {
            return executable.getPath();
        }

        return new File(jvm).getPath();
    }

    boolean isNativeVm()
    {
        return !executable.equals(RUN_JAR);
    }

    private static File findRunBinary()
    {
        final Path targetDir = Paths.get("target");
        if (!targetDir.toFile().exists())
        {
            return NOT_FOUND;
        }

        final File aotBinary = targetDir.resolve("benchmarks").toFile();
        if (aotBinary.exists())
        {
            return aotBinary;
        }

        final File instrumentedBinary = targetDir
            .resolve("benchmarks.output")
            .resolve("default")
            .resolve("benchmarks")
            .toFile();

        if (instrumentedBinary.exists())
        {
            return instrumentedBinary;
        }

        return NOT_FOUND;
    }

    private static boolean isNativeVm(File runBinary)
    {
        if (RUN_JAR.exists() && runBinary.exists())
        {
            return RUN_JAR.lastModified() <= runBinary.lastModified();
        }

        if (RUN_JAR.exists())
        {
            return false;
        }

        return runBinary.exists();
    }

    private static String binaryReadString(String key, File binary)
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
}
