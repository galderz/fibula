package org.mendrugo.fibula.extension.deployment;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.GraalVM;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.logging.Log;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathTree;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.generators.core.SourceError;
import org.openjdk.jmh.generators.reflection.RFGeneratorSource;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.util.FileUtils;

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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
    void runtimeInit(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit)
    {
        // Make InfraControl runtime initialized because it uses unsafe
        // to calculate field offsets during a static initializer.
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.openjdk.jmh.runner.InfraControl"));
    }

    @BuildStep
    void reflection(BuildProducer<ReflectiveClassBuildItem> reflection)
    {
        // Register org.openjdk.jmh.runner.InfraControl fields for reflection
        // so that field offset calculations that rely on getting declared fields with reflection,
        // work as expected.
        // All classes in the InfraControl hierarchy need to be registered for reflection
        // so that the gaps between the fields are maintained.
        reflection.produce(ReflectiveClassBuildItem
            .builder(
                "org.openjdk.jmh.runner.InfraControlL0"
                , "org.openjdk.jmh.runner.InfraControlL1"
                , "org.openjdk.jmh.runner.InfraControlL2"
                , "org.openjdk.jmh.runner.InfraControlL3"
                , "org.openjdk.jmh.runner.InfraControlL4"
            )
            .fields()
            .build()
        );
    }

    @BuildStep
    void exportGraalCompilerApi(
        BuildProducer<JPMSExportBuildItem> jpmsExports
        , NativeImageRunnerBuildItem nativeImageRunnerBuildItem
    )
    {
        final GraalVM.Version graalVMVersion = nativeImageRunnerBuildItem.getBuildRunner().getGraalVMVersion();
        if (graalVMVersion.compareTo(GraalVM.Version.VERSION_23_1_0) <= 0)
        {
            jpmsExports.produce(new JPMSExportBuildItem(
                "jdk.internal.vm.compiler"
                , "org.graalvm.compiler.api.directives")
            );
        }
        else
        {
            jpmsExports.produce(new JPMSExportBuildItem(
                "jdk.graal.compiler"
                , "jdk.graal.compiler.api.directives")
            );
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void linkBlackholeToGraal(
        BuildProducer<GeneratedClassBuildItem> generatedClasses
        , NativeImageRunnerBuildItem nativeImageRunnerBuildItem
    )
    {
        Log.info("Generating blackhole substitution");
        final ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        generateBlackholeSubstitution(classOutput, nativeImageRunnerBuildItem.getBuildRunner().getGraalVMVersion());
    }

    @BuildStep
    void generateBenchmarks(
        BuildProducer<GeneratedClassBuildItem> generatedClasses
        , CombinedIndexBuildItem index
        , BuildSystemTargetBuildItem buildSystemTarget
        , BuildProducer<ReflectiveClassBuildItem> reflection
        , CurateOutcomeBuildItem curateOutcomeBuildItem
    )
    {
        Log.info("Generate benchmarks");
        final BenchmarksPaths benchmarksPaths = generateBenchmarksFromBytecode(buildSystemTarget.getOutputDirectory());

        Log.info("Compile benchmarks");
        final List<GeneratedClassBuildItem> compiled = compileBenchmarks(benchmarksPaths, curateOutcomeBuildItem.getApplicationModel());
        compiled.forEach(generatedClasses::produce);
        Log.infof("Compiled %d classes", compiled.size());
        Log.debugf(
            "Compiled classes are: %s"
            , compiled.stream().map(GeneratedClassBuildItem::getName).sorted().collect(Collectors.joining(", "))
        );

        Log.info("Copy compiled benchmarks to test classes");
        copyCompiledClassesToTest(benchmarksPaths.generatedClassesDir, buildSystemTarget.getOutputDirectory().resolve("test-classes"));
        // TODO move info message and print number of copied classes

        final Set<String> jmhTestsCompiled = compiled.stream()
            .map(GeneratedClassBuildItem::getName)
            .filter(name -> name.endsWith("jmhTest"))
            .map(name -> name.replace('/', '.'))
            .collect(Collectors.toSet());
        Log.infof("Found %d generated benchmarks in compiled code", jmhTestsCompiled.size());
        Log.debugf(
            "Compiled benchmarks are: %s"
            , jmhTestsCompiled.stream().sorted().collect(Collectors.joining(", "))
        );
        final Collection<String> generatedBenchmarkFQNs = new HashSet<>(jmhTestsCompiled);

        // TODO is this needed?
        final Set<String> jmhTestsInJandex = index.getIndex().getKnownClasses().stream()
            .filter(classInfo -> classInfo.name().toString().endsWith("jmhTest"))
            .filter(classInfo -> isSupported(classInfo.name().toString()))
            .map(info -> info.name().toString())
            .collect(Collectors.toSet());
        Log.infof("Found %d generated benchmarks in jandex", jmhTestsInJandex.size());
        Log.debugf(
            "Generated benchmarks in jandex are: %s"
            , jmhTestsInJandex.stream().sorted().collect(Collectors.joining(", "))
        );
        generatedBenchmarkFQNs.addAll(jmhTestsInJandex);

        Log.info("Register generated benchmarks for reflection");
        generatedBenchmarkFQNs.stream()
            .map(ReflectiveClassBuildItem::builder)
            .map(builder -> builder.methods(true).build())
            .forEach(reflection::produce);

        Log.infof("Collect BenchmarkList metadata from dependencies and project");
        collectBenchmarkList(curateOutcomeBuildItem.getApplicationModel(), buildSystemTarget.getOutputDirectory());
    }

    // TODO try to do it more neatly
    private void copyCompiledClassesToTest(Path generatedClassesDir, Path testClassesPath)
    {
        // This helps with integration testing.
        // It makes the classes are also available in a path that will be used by default when integration testing.
        try (Stream<Path> walk = Files.walk(generatedClassesDir))
        {
            walk.forEach(source ->
            {
                try
                {
                    String targetSubPath = source.toString().substring(generatedClassesDir.toString().length());
                    if (!targetSubPath.isEmpty())
                    {
                        // Remove / at the start of the path
                        targetSubPath = targetSubPath.substring(1);

                        final Path target = testClassesPath.resolve(targetSubPath);
                        final File sourceFile = source.toFile();
                        final File targetFile = target.toFile();
                        if (sourceFile.isDirectory())
                        {
                            if (targetFile.exists())
                            {
                                Log.debugf("Skip replacing directory %s because it exists", target);
                            }
                            else
                            {
                                final boolean mkdirResult = targetFile.mkdirs();
                                Log.debugf("Make directory %s with success result %s", target, mkdirResult);
                            }
                        }
                        else
                        {
                            Log.debugf("Copying %s to %s", source, target);
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            });
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private BenchmarksPaths generateBenchmarksFromBytecode(Path buildDir)
    {
        final Path classesDir = buildDir.resolve("classes");
        final Path sourceDirectory = buildDir.resolve("generated-sources").resolve("bc");
        final Path generatedClassesDir = buildDir.resolve("generated-classes");
        final BenchmarksPaths paths = new BenchmarksPaths(classesDir, sourceDirectory, generatedClassesDir);

        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try
        {
            final URLClassLoader amendedCL = new URLClassLoader(
                new URL[]{paths.classesDir.toFile().toURI().toURL()},
                Thread.currentThread().getContextClassLoader());

            Thread.currentThread().setContextClassLoader(amendedCL);

            final FileSystemDestination destination = new FileSystemDestination(
                paths.generatedClassesDir.toFile()
                , paths.sourceDirectory.toFile()
            );

            Collection<File> classes = FileUtils.getClasses(paths.classesDir.toFile());
            Log.infof(
                "Processing %d classes from %s with reflection generator"
                , classes.size()
                , paths.classesDir.toFile()
            );
            Log.infof(
                "Writing out Java source to %s and resources to %s"
                , paths.sourceDirectory
                , paths.generatedClassesDir
            );

            final RFGeneratorSource source = new RFGeneratorSource();
            for (File f : classes)
            {
                String name = f.getAbsolutePath().substring(paths.classesDir.toFile().getAbsolutePath().length() + 1);
                name = name.replaceAll("\\\\", ".");
                name = name.replaceAll("/", ".");
                if (name.endsWith(".class"))
                {
                    source.processClasses(Class.forName(name.substring(0, name.length() - 6), false, amendedCL));
                }
            }

            final BenchmarkGenerator generator = new BenchmarkGenerator();
            generator.generate(source, destination);
            generator.complete(source, destination);

            if (destination.hasErrors())
            {
                for (SourceError e : destination.getErrors())
                {
                    System.err.println(e.toString() + "\n");
                }
                throw new RuntimeException("Failure to generate benchmarks, see errors above");
            }

            // Restore classloader
            return paths;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private void collectBenchmarkList(ApplicationModel applicationModel, Path outputDirectory)
    {
        final Path benchmarkListPath = outputDirectory
            .resolve("classes")
            .resolve(BenchmarkList.BENCHMARK_LIST.substring(1));
        benchmarkListPath.getParent().toFile().mkdirs();

        appendBenchmarkListInClassPath(applicationModel, benchmarkListPath);
        appendToBenchmarkList(outputDirectory.resolve("generated-classes/META-INF/BenchmarkList"), benchmarkListPath);
    }

    public static void appendBenchmarkListInClassPath(ApplicationModel applicationModel, Path benchmarkListPath)
    {
        for (ResolvedDependency dependency : applicationModel.getDependencies())
        {
            final PathTree tree = dependency.getContentTree(new PathFilter(
                List.of("**/BenchmarkList")
                , List.of())
            );
            try(final OpenPathTree open = tree.open())
            {
                open.walk(visit -> {
                    Log.infof("Found %s in %s", visit.getPath(), dependency);
                    appendToBenchmarkList(visit.getPath(), benchmarkListPath);
                });
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void appendToBenchmarkList(Path path, Path benchmarkListPath)
    {
        try
        {
            for (String line : Files.readAllLines(path))
            {
                Files.writeString(
                    benchmarkListPath
                    , line + System.lineSeparator()
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.APPEND
                );
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private List<GeneratedClassBuildItem> compileBenchmarks(BenchmarksPaths paths, ApplicationModel applicationModel)
    {
        final Set<File> javaFiles = classFilesInDirectory(paths.sourceDirectory);
        final Set<Path> dependencyPaths = applicationModel.getRuntimeDependencies().stream()
            .map(ResolvedDependency::getResolvedPaths)
            .map(PathCollection::getSinglePath)
            .collect(Collectors.toSet());

        final List<Path> classPath = new ArrayList<>();
        classPath.add(paths.classesDir);
        classPath.addAll(dependencyPaths);
        Log.debugf("Compile classpath is: %s", classPath);

        return compile(javaFiles, classPath, paths);
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

    public List<GeneratedClassBuildItem> compile(Set<File> files, List<Path> classPath, BenchmarksPaths paths)
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
                , List.of(paths.generatedClassesDir.toFile())
            );

            final Iterable<? extends JavaFileObject> compilationUnit = fileManager
                .getJavaFileObjects(files.toArray(new File[0]));

            final JavaCompiler.CompilationTask task = compiler.getTask(
                null // a writer for additional output from the compiler; use System.err if null
                , fileManager
                , diagnostics
                , List.of("-proc:none") // no annotation processing
                , null // names of classes to be processed by annotation processing, null means no classes
                , compilationUnit
            );

            boolean success = task.call();
            if (!success)
            {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
                {
                    Log.errorf("Error on line %d in %s: %s%n"
                        , diagnostic.getLineNumber()
                        , diagnostic.getSource() == null ? "NA" : diagnostic.getSource().toUri()
                        , diagnostic.getMessage(Locale.ENGLISH)
                    );
                }
                return Collections.emptyList();
            }
            else
            {
                Log.info("Compilation success.");
                return compiledClassItems(paths.generatedClassesDir);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    List<GeneratedClassBuildItem> compiledClassItems(Path generatedClassesDir)
    {
        try(final Stream<Path> walk = Files.walk(generatedClassesDir))
        {
            return walk
                .filter(path -> !Files.isDirectory(path))
                .map(path -> toGeneratedClassBuildItem(path, generatedClassesDir))
                .toList();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static GeneratedClassBuildItem toGeneratedClassBuildItem(Path file, Path base)
    {
        try
        {
            final Path relativePath = base.relativize(file);
            final String withoutExtension = relativePath
                .getFileName().toString()
                .replaceFirst("\\.class$", "");
            final Path path = relativePath.getParent().resolve(withoutExtension);
            final byte[] classData = Files.readAllBytes(file);
            Log.debugf("Generate class build item for: %s", path);
            return new GeneratedClassBuildItem(true, path.toString(), classData);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void generateBlackholeSubstitution(ClassOutput classOutput, GraalVM.Version graalVMVersion)
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

            final String graalCompilerPackagePrefix =
                graalVMVersion.compareTo(GraalVM.Version.VERSION_23_1_0) <= 0
                ? "org.graalvm"
                : "jdk.graal";

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

    // todo do I still need this?
    private static boolean isSupported(String fqn)
    {
        if (fqn.startsWith("org.mendrugo.fibula"))
        {
            return true;
        }

        if (fqn.startsWith("org.openjdk.jmh.it"))
        {
            return true;
        }

        if (fqn.startsWith("org.openjdk.jmh.samples"))
        {
            return fqn.contains("JMHSample_01")
                || fqn.contains("JMHSample_03")
                || fqn.contains("JMHSample_04")
                || fqn.contains("JMHSample_09");
        }

        return true;
    }

    record BenchmarksPaths(
        Path classesDir // compiled bytecode directory
        , Path sourceDirectory // output source directory
        , Path generatedClassesDir // output resources directory
    ) {}
}

