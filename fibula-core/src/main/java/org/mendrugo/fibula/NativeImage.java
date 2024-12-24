package org.mendrugo.fibula;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class NativeImage
{
    private static final File EXECUTABLE = findExecutable();

    private final Execute execute;

    public NativeImage(Execute execute)
    {
        this.execute = execute;
    }

    int getJavaVersion()
    {
        if (!EXECUTABLE.exists())
        {
            return 0;
        }

        final List<String> commandString = new ArrayList<>();
        commandString.add(EXECUTABLE.getAbsolutePath());
        commandString.add("--version");
        return GraalVersionParser.parse(execute.execute(commandString).stream());
    }

    void execute(String... args)
    {
        final List<String> commandString = new ArrayList<>();
        commandString.add(EXECUTABLE.getAbsolutePath());
        commandString.addAll(Arrays.asList(args));
        execute.execute(commandString);
    }

    private static File findExecutable()
    {
        // todo add Windows support
        final String executableName = "native-image";
        final String graalvmHomeEnv = System.getenv("GRAALVM_HOME");
        if (null != graalvmHomeEnv)
        {
            File file = Paths.get(graalvmHomeEnv, "bin", executableName).toFile();
            if (file.exists())
            {
                return file.toPath().toFile();
            }
        }

        Path javaHome = findJavaHome();
        if (javaHome != null)
        {
            final File execFile = javaHome.resolve("bin").resolve(executableName).toFile();
            if (execFile.exists())
            {
                return execFile.toPath().toFile();
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
                        return execFile.toPath().toFile();
                    }
                }
            }
        }

        return new File("NOT_FOUND");
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
