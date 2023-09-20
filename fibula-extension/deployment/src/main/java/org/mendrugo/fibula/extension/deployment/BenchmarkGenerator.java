package org.mendrugo.fibula.extension.deployment;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.mendrugo.fibula.results.ThroughputResult;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class BenchmarkGenerator
{
    static final String PACKAGE_NAME = "org.mendrugo.fibula.gen";

    void generate(Map<ClassInfo, List<MethodInfo>> benchmarkInfo)
    {
        MethodSpec.Builder mainBuilder = MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(void.class)
            .addParameter(String[].class, "args");
        // .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
        // .build();

        // benchmarkInfo.map(this::generateClass);
        benchmarkInfo.entrySet().stream()
            .map(this::generateBenchmarkClass)
            .forEach(benchClass -> mainBuilder.addStatement("$S.main()", benchClass.name));

        TypeSpec runner = TypeSpec.classBuilder("FibulaRunner")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(mainBuilder.build())
            .build();

        JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, runner).build();
        writeToPath(javaFile);
    }

    private TypeSpec generateBenchmarkClass(Map.Entry<ClassInfo, List<MethodInfo>> entry)
    {
        final ClassInfo classInfo = entry.getKey();
        final List<MethodInfo> methodInfos = entry.getValue();

        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(classInfo.simpleName() + "_Fibula")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        MethodSpec.Builder mainBuilder = MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(void.class);

        methodInfos.stream()
            .map(this::generateBenchmarkMethod)
            .peek(benchMethod -> mainBuilder.addStatement("$S()", benchMethod.name))
            .forEach(typeBuilder::addMethod);

        typeBuilder.addMethod(mainBuilder.build());

        final TypeSpec type = typeBuilder.build();
        final JavaFile javaFile = JavaFile.builder(PACKAGE_NAME, type).build();
        writeToPath(javaFile);

        return type;
    }

    private static void writeToPath(JavaFile javaFile)
    {
        try
        {
            javaFile.writeTo(Path.of("target/generated-sources/fibula"));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    MethodSpec generateBenchmarkMethod(MethodInfo method)
    {
        return MethodSpec.methodBuilder(method.name() + "_Throughput")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(ThroughputResult.class)
            .addStatement("$T obj = new $T()", method.declaringClass().name())
            // .addStatement("$T resultBuilder = new $T();", TaskResultBuilder.class)
            .addStatement("long operations = 0")
            .addStatement("long startTime = $T.nanoTime()", System.class)
            .beginControlFlow("do")
            .addStatement("obj.$S()", method.name())
            .addStatement("operations++")
            .endControlFlow("while(!control.isDone)")
            .addStatement("long stopTime = $T.nanoTime()", System.class)
            .addStatement("return ThroughputResult.of($S, operations, stopTime, startTime)", method.name())
            .build();
    }
}
