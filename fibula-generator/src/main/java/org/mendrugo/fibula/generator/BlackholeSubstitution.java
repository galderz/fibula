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
    static void generate(GraalBlackhole graalBlackhole, ClassOutput classOutput)
    {
        generateBlackholeSubstitution(classOutput, graalBlackhole);
    }

    private static void generateBlackholeSubstitution(ClassOutput classOutput, GraalBlackhole graalBlackhole)
    {
        final String className = String.format(
            "%s.Target_org_openjdk_jmh_infra_Blackhole"
            , NativeAssetsGenerator.PACKAGE_NAME
        );

        try (final ClassCreator blackhole = ClassCreator.builder()
            .classOutput(classOutput)
            .className(className)
            .setFinal(true)
            .build()
        ) {
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
}
