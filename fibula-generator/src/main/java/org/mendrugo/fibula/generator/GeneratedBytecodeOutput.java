package org.mendrugo.fibula.generator;

import io.quarkus.gizmo.ClassOutput;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeneratedBytecodeOutput implements ClassOutput
{
    private final Path classOutputPath;

    public GeneratedBytecodeOutput(Path classOutputPath)
    {
        this.classOutputPath = classOutputPath;
    }

    @Override
    public void write(String name, byte[] data)
    {
        try
        {
            final File dir = classOutputPath
                .resolve(name.substring(0, name.lastIndexOf("/")))
                .toFile();
            dir.mkdirs();

            final File output = classOutputPath
                .resolve(name + ".class")
                .toFile();

            Files.write(output.toPath(), data);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot dump the class: " + name, e);
        }
    }

    @Override
    public Writer getSourceWriter(String className)
    {
        final Path outputDirectory = classOutputPath.getParent();
        final Path gizmoSourcesPath = outputDirectory
            .resolve("generated-sources")
            .resolve("gizmo");

        final File dir = gizmoSourcesPath
            .resolve(className.substring(0, className.lastIndexOf('/')))
            .toFile();
        dir.mkdirs();

        final File output = gizmoSourcesPath
            .resolve(className + ".zig")
            .toFile();

        try
        {
            return Files.newBufferedWriter(output.toPath());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot write .zig file for " + className, e);
        }
    }
}
