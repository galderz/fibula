package org.mendrugo.fibula.extension.deployment;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.logging.Log;
import org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator;
import org.openjdk.jmh.generators.core.JmhBenchmarkGenerator;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BenchmarkProcessor
{
    private static final String FEATURE = "fibula-extension";
    private static final String PACKAGE_NAME = "org.mendrugo.fibula.generated";

    @BuildStep
    FeatureBuildItem feature()
    {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void generateBenchmarks(
        BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses
        , OutputTargetBuildItem outputTargetBuildItem
        , BuildProducer<GeneratedClassBuildItem> generatedClasses
        , CombinedIndexBuildItem index
        , BuildSystemTargetBuildItem buildSystemTarget
    )
    {
        final ClassOutput beanOutput = new GeneratedBeanGizmoAdaptor(generatedBeanClasses);
        final ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);

        Log.info("Generating blackhole substitution");
        generateBlackholeSubstitution(classOutput);

        Log.info("Compile benchmarks");
        final List<GeneratedClassBuildItem> compiled = compileBenchmarks(outputTargetBuildItem.getOutputDirectory());
        compiled.forEach(generatedClasses::produce);

        Log.info("Generate benchmark injections");
        final JandexGeneratorSource source = new JandexGeneratorSource(index.getIndex());
        final JmhBenchmarkGenerator generator = new JmhBenchmarkGenerator(beanOutput, classOutput);
        generator.generate(source);
        generator.complete(buildSystemTarget);
    }

//    private void generateBenchmarkInjections()
//    {
//        Modes.nonAll().forEach(benchmarkKind ->
//        {
//            final String prefix = String.format(
//                "%s.%s_%s_%s"
//                , info.generatedPackageName
//                , info.generatedClassName
//                , info.methodGroup.getName()
//                , benchmarkKind.name()
//            );
//
//            final String functionFqn = String.format("%s_Function", prefix);
//
//            try (final ClassCreator function = ClassCreator.builder()
//                .classOutput(classOutput)
//                .className(functionFqn)
//                .interfaces(Function.class)
//                .build()
//            )
//            {
//                addMethods(benchmarkKind, info.methodGroup, states, function);
//
//                // Write out state initializers
//                states.addStateInitializers(function);
//            }
//
//            final String supplierFqn = String.format("%s_Supplier", prefix);
//            generateBenchmarkSupplier(functionFqn, supplierFqn);
//        });
//    }

    private List<GeneratedClassBuildItem> compileBenchmarks(Path buildDir)
    {
        final Path sourceDirectory = buildDir.resolve("generated-sources").resolve("bc");
        final Path classesDir = buildDir.resolve("classes");
        final Path generatedClassesDir = buildDir.resolve("generated-classes");
        final List<Path> pathArgs = List.of(
            classesDir // compiled bytecode directory
            , sourceDirectory // output source directory
            , generatedClassesDir // output resources directory
        );

        final String[] bytecodeGenArgs = pathArgs.stream()
            .map(Path::toString)
            .toArray(String[]::new);

        try
        {
            // todo use asm/reflection generators directly to avoid potential for System.exit() calls
            final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            JmhBytecodeGenerator.main(bytecodeGenArgs);
            // Restore classloader manually because wrapper doesn't do so
            Thread.currentThread().setContextClassLoader(tccl);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        final Set<File> javaFiles = classFilesInDirectory(sourceDirectory);
        // todo use the ApplicationModel to extract the classpath
        final List<Path> classPath = List.of(
            classesDir
            , Path.of(System.getProperty("user.home"))
                .resolve(".m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar")
        );
        return compile(javaFiles, classPath, generatedClassesDir, sourceDirectory);
    }

    private static Set<File> classFilesInDirectory(Path dir)
    {
        try (Stream<Path> javaPaths = Files.find(
            dir
            , Integer.MAX_VALUE
            , (path, attr) -> path.toString().endsWith(".java")
        ))
        {
            return javaPaths.map(Path::toFile)
                .collect(Collectors.toSet());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public List<GeneratedClassBuildItem> compile(Set<File> files, List<Path> classPath, Path classOutputDir, Path sourceDirectory)
    {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assert compiler != null : "no system java compiler available - JDK is required!";

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnostics
            , Locale.getDefault()
            , StandardCharsets.UTF_8
        ))
        {
            fileManager.setLocation(
                StandardLocation.CLASS_PATH
                , classPath.stream().map(Path::toFile).toList()
            );
            fileManager.setLocation(
                StandardLocation.CLASS_OUTPUT
                , List.of(classOutputDir.toFile())
            );

            final Iterable<? extends JavaFileObject> compilationUnit = fileManager
                .getJavaFileObjects(files.toArray(new File[0]));

            final JavaCompiler.CompilationTask task = compiler.getTask(
                null // a writer for additional output from the compiler; use System.err if null
                , fileManager
                , diagnostics
                , null // no compiler options
                , null // names of classes to be processed by annotation processing, null means no classes
                , compilationUnit
            );

            boolean success = task.call();
            if (!success)
            {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
                {
                    System.out.format("Error on line %d in %s: %s%n"
                        , diagnostic.getLineNumber()
                        // , diagnostic.getSource().toUri()
                        , diagnostic.getSource() == null ? "NA" : diagnostic.getSource().toUri()
                        , diagnostic.getMessage(Locale.ENGLISH)
                    );
                }
                return Collections.emptyList();
            }
            else
            {
                System.out.println("Compilation success.");
                return compiledClassItems(sourceDirectory, compilationUnit.iterator().next(), classOutputDir);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    List<GeneratedClassBuildItem> compiledClassItems(Path sourceDirectory, JavaFileObject javaFileObject, Path classesDir)
    {
        final Path absoluteSourceFilePath = Path.of(javaFileObject.toUri());
        final Path relativeSourceFilePath = absoluteSourceFilePath.subpath(sourceDirectory.getNameCount(), absoluteSourceFilePath.getNameCount());
        final Path generatedPackagePath = relativeSourceFilePath.subpath(0, relativeSourceFilePath.getNameCount() - 1);

        Log.debugf("Relative source file path: %s", relativeSourceFilePath);
        Log.debugf("Generated package path: %s", generatedPackagePath);

        Path jmhGeneratedClassDir = classesDir.resolve(generatedPackagePath);
        try(Stream<Path> files = Files.list(jmhGeneratedClassDir))
        {
            return files
                .map(classFile -> compiledClassItem(classFile, generatedPackagePath))
                .toList();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static GeneratedClassBuildItem compiledClassItem(Path file, Path packagePath)
    {
        try
        {
//            final String name = String.format(
//                "%s.%s"
//                , packagePath.toString().replace(File.separatorChar, '.')
//                , file.getFileName().toString().replaceAll("(?<!^)[.].*", "")
//            );

            final String name = String.format(
                "%s/%s"
                , packagePath.toString()
                , file.getFileName().toString().replaceAll("(?<!^)[.].*", "")
            );
            final byte[] classData = Files.readAllBytes(file);
            Log.debugf("Generate class build item for: %s", name);
            return new GeneratedClassBuildItem(true, name, classData);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

//    @BuildStep
//    void generateCommand(
//        BuildProducer<GeneratedBeanBuildItem> generatedBeanClasses
//        , BuildProducer<GeneratedClassBuildItem> generatedClasses
//        , CombinedIndexBuildItem index
//        , BuildSystemTargetBuildItem buildSystemTarget
//    )
//    {
//        final ClassOutput beanOutput = new GeneratedBeanGizmoAdaptor(generatedBeanClasses);
//        final ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
//
//        final JandexGeneratorSource source = new JandexGeneratorSource(index.getIndex());
//        final JmhBenchmarkGenerator generator = new JmhBenchmarkGenerator(beanOutput, classOutput);
//        generator.generate(source);
//        generator.complete(buildSystemTarget);
//
//        Log.info("Generating blackhole substitution");
//        generateBlackholeSubstitution(classOutput);
//    }

    private void generateBlackholeSubstitution(ClassOutput classOutput)
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
            blackhole.addAnnotation(TargetClass.class).add("className", "org.openjdk.jmh.infra.Blackhole");

            final String graalCompilerPackagePrefix = System.getProperty("fibula.graal.compiler.package.prefix", "org.graalvm");

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
            consumeParameterTypes.forEach(clazz -> generateBlackholeConsumeSubstitution(clazz, graalCompilerPackagePrefix, blackhole));
        }
    }

    private static void generateBlackholeConsumeSubstitution(Class<?> clazz, String graalCompilerPackagePrefix, ClassCreator blackhole)
    {
        try (final MethodCreator consume = blackhole.getMethodCreator("consume", void.class, clazz))
        {
            consume.addAnnotation(Substitute.class);

            final ResultHandle value = consume.getMethodParam(0);
            consume.invokeStaticMethod(MethodDescriptor.ofMethod(graalCompilerPackagePrefix + ".compiler.api.directives.GraalDirectives", "blackhole", void.class, clazz), value);
            consume.returnVoid();
        }
    }
}

