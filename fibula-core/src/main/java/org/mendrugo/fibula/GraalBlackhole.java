package org.mendrugo.fibula;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public enum GraalBlackhole
{
    DISABLED("")
    , PRE_JDK_21("org.graalvm.compiler.api.directives.GraalDirectives")
    , POST_JDK_21("jdk.graal.compiler.api.directives.GraalDirectives");

    private final String compilerDirectivesQName;

    GraalBlackhole(String compilerDirectivesQName)
    {
        this.compilerDirectivesQName = compilerDirectivesQName;
    }

    public String getCompilerDirectivesQName()
    {
        return compilerDirectivesQName;
    }

    public boolean isEnabled()
    {
        return !isDisabled();
    }

    public boolean isDisabled()
    {
        return this == DISABLED;
    }

    @Override
    public String toString()
    {
        switch (this)
        {
            case PRE_JDK_21:
                return "enabled (JDK 21 or earlier)";
            case POST_JDK_21:
                return "enabled (JDK 22 or later)";
           default:
               return "disabled";
        }
    }

    public static GraalBlackhole instance()
    {
        final int graalVMJavaVersion = getGraalVMJavaVersion();
        if (0 == graalVMJavaVersion)
        {
            return DISABLED;
        }

        if (graalVMJavaVersion <= 21)
        {
            return PRE_JDK_21;
        }

        return POST_JDK_21;
    }

    private static int getGraalVMJavaVersion()
    {
        final Path nativeImageExecutable = findNativeImageExecutable();
        if (null == nativeImageExecutable)
        {
            return 0;
        }

        final String[] versionCommand = {
            nativeImageExecutable.toAbsolutePath().toString()
            , "--version"
        };

        try
        {
            final Process versionProcess = new ProcessBuilder(versionCommand)
                .redirectErrorStream(true)
                .start();
            versionProcess.waitFor();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(versionProcess.getInputStream(), StandardCharsets.UTF_8)
            ))
            {
                return GraalVersionParser.parse(reader.lines());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to get GraalVM version", e);
        }
    }

    private static Path findNativeImageExecutable()
    {
        // todo add Windows support
        final String executableName = "native-image";
        final String graalvmHomeEnv = System.getenv("GRAALVM_HOME");
        if (null != graalvmHomeEnv)
        {
            File file = Paths.get(graalvmHomeEnv, "bin", executableName).toFile();
            if (file.exists())
            {
                return file.toPath();
            }
        }

        Path javaHome = findJavaHome();
        if (javaHome != null)
        {
            final File execFile = javaHome.resolve("bin").resolve(executableName).toFile();
            if (execFile.exists())
            {
                return execFile.toPath();
            }
        }

        final String systemPath = System.getenv("PATH");
        if (systemPath != null)
        {
            final String[] pathDirs = systemPath.split(File.pathSeparator);
            for (String pathDir : pathDirs)
            {
                final File dir = new File(pathDir);
                if (dir.isDirectory()) {
                    final File execFile = new File(dir, executableName);
                    if (execFile.exists())
                    {
                        return execFile.toPath();
                    }
                }
            }
        }

        return null;
    }

    private static Path findJavaHome()
    {
        String home = System.getProperty("java.home");
        if (home == null)
        {
            home = System.getenv("JAVA_HOME");
        }

        if (home != null)
        {
            return Paths.get(home);
        }

        return null;
    }
}
