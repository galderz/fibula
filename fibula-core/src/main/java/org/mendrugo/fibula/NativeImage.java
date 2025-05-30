package org.mendrugo.fibula;

import org.openjdk.jmh.runner.OutputFormatAdapter;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.InputStreamDrainer;
import org.openjdk.jmh.util.TempFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class NativeImage
{
    private static final File EXECUTABLE = findExecutable();

    private final boolean verbosePrint;
    private final OutputFormat out;

    public NativeImage(boolean verbosePrint, OutputFormat out)
    {
        this.verbosePrint = verbosePrint;
        this.out = out;
    }

    static int getJavaVersion()
    {
        if (!EXECUTABLE.exists())
        {
            return 0;
        }

        final List<String> commandString = new ArrayList<>();
        commandString.add(EXECUTABLE.getAbsolutePath());
        commandString.add("--version");
        try
        {
            final Process process = new ProcessBuilder(commandString).start();
            return GraalVersionParser.parse(process.getInputStream());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    void execute(String... args)
    {
        final List<String> commandString = new ArrayList<>();
        commandString.add(EXECUTABLE.getAbsolutePath());
        commandString.addAll(Arrays.asList(args));

        try
        {
            final TempFile tmpFile = FileUtils.weakTempFile("nativeimage");

            try (FileOutputStream fos = new FileOutputStream(tmpFile.file()))
            {
                final ProcessBuilder processBuilder = new ProcessBuilder(commandString);
                final Process process = processBuilder.start();

                final InputStreamDrainer errDrainer = new InputStreamDrainer(process.getErrorStream(), fos);
                final InputStreamDrainer outDrainer = new InputStreamDrainer(process.getInputStream(), fos);

                if (verbosePrint)
                {
                    errDrainer.addOutputStream(new OutputFormatAdapter(out));
                    outDrainer.addOutputStream(new OutputFormatAdapter(out));
                }

                errDrainer.start();
                outDrainer.start();

                process.waitFor();

                errDrainer.join();
                outDrainer.join();
            }
            catch (InterruptedException e)
            {
                out.println("<interrupted waiting for native image: " + e.getMessage() + ">");
                out.println("");
                throw new RuntimeException(e);
            }

            tmpFile.delete();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
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
