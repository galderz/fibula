package org.mendrugo.fibula.generator;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.mendrugo.fibula.GraalBlackhole;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BlackholeSubstitution
{
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";

    static void generate(GraalBlackhole graalBlackhole, Path classOutputPath)
    {
        generateBlackholeSubstitution(new BlackholeClassOutput(classOutputPath), graalBlackhole);
    }

    private static void generateBlackholeSubstitution(ClassOutput classOutput, GraalBlackhole graalBlackhole)
    {
        final String className = String.format(
            "%s.Target_org_openjdk_jmh_infra_Blackhole"
            , PACKAGE_NAME
        );

        try (final ClassCreator blackhole = ClassCreator.builder()
            .classOutput(classOutput)
            .className(className)
            .setFinal(true)
            .build()
        )
        {
            blackhole.addAnnotation("com.oracle.svm.core.annotate.TargetClass")
                .add("className", "org.openjdk.jmh.infra.Blackhole");

            List<Class<?>> consumeParameterTypes = List.of(
                byte.class
                , boolean.class
                , char.class
                , double.class
                , float.class
                , int.class
                , long.class
                , short.class
                , Object.class
            );
            consumeParameterTypes.forEach(clazz ->
                generateBlackholeConsumeSubstitution(clazz, graalBlackhole, blackhole)
            );
        }
    }

    private static void generateBlackholeConsumeSubstitution(Class<?> clazz, GraalBlackhole graalBlackhole, ClassCreator blackhole)
    {
        try (final MethodCreator consume = blackhole.getMethodCreator("consume", void.class, clazz))
        {
            consume.addAnnotation("com.oracle.svm.core.annotate.Substitute");

            final ResultHandle value = consume.getMethodParam(0);
            final MethodDescriptor blackholeMethod = MethodDescriptor.ofMethod(
                graalBlackhole.getCompilerDirectivesQName()
                , "blackhole"
                , void.class
                , clazz
            );
            consume.invokeStaticMethod(blackholeMethod, value);
            consume.returnVoid();
        }
    }

    private static final class BlackholeClassOutput implements ClassOutput
    {
        private final Path classOutputPath;

        public BlackholeClassOutput(Path classOutputPath)
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
}
