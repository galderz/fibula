package org.mendrugo.fibula.generator;

import org.mendrugo.fibula.GraalBlackhole;
import org.openjdk.jmh.generators.BenchmarkProcessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * An annotation processor that combines the benchmark source code and BenchmarkList metadata generation in JMH,
 * with the need to generate GraalVM configuration and bytecode for JMH benchmarks as native executables.
 * Running both processors independently won't work because the JMH processor needs to run first
 * in order for the native configuration generator to pick up the names of the JMH generated classes.
 * Running processors independently with the JMH one running first will only work if there are unmatched annotations.
 * This is difficult to guarantee which is we use a single processor that combines functionalities from both.
 */
@SupportedAnnotationTypes("org.openjdk.jmh.annotations.*")
public class NativeAssetsGenerator extends AbstractProcessor
{
    final BenchmarkProcessor jmhBenchmarkProcessor;
    // Set of BenchmarkList files in dependencies
    final Set<URI> benchmarkLists = new HashSet<>();
    final List<String> benchmarkQNames = new ArrayList<>();

    public NativeAssetsGenerator()
    {
        this.jmhBenchmarkProcessor = new BenchmarkProcessor();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        jmhBenchmarkProcessor.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        try
        {
             // Order of processor invocation is important here.
             // The JMH processor has to run first, then the native assets generator
             // so that it picks up the JMH generated classes.
            jmhBenchmarkProcessor.process(annotations, roundEnv);

            if (!roundEnv.processingOver())
            {
                findBenchmarkLists();
                findBenchmarksInProject(roundEnv);
            }
            else
            {
                findBenchmarksInDependencies();
                writeReflectionConfiguration();
                final Path classOutputPath = getClassOutputPath();
                appendBenchmarkList(classOutputPath);
                generateBlackholeSubstitutions(classOutputPath);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException(e);
        }
        return true;
    }

    private void generateBlackholeSubstitutions(Path classOutputPath)
    {
        final GraalBlackhole graalBlackhole = GraalBlackhole.instance();
        printNote("GraalVM blackhole mode resolved: " + graalBlackhole);
        if (graalBlackhole.isEnabled())
        {
            BlackholeSubstitution.generate(graalBlackhole, classOutputPath);
        }
    }

    private void appendBenchmarkList(Path classOutputPath) throws IOException
    {
        printNote("Append to BenchmarkList");

        final Path benchmarkListPath = classOutputPath.resolve("META-INF/BenchmarkList");

        for (URI uri : benchmarkLists)
        {
            appendBenchmarkList(uri, benchmarkListPath);
        }
    }

    private Path getClassOutputPath() throws IOException
    {
        final FileObject ignore = processingEnv
            .getFiler()
            .createResource(
                StandardLocation.CLASS_OUTPUT
                , ""
                , "ignore"
            );

        return Paths.get(ignore.toUri()).getParent();
    }

    private void writeReflectionConfiguration() throws IOException
    {
        printNote("Write reflection configuration");
        final FileObject reflectionConfig = processingEnv
            .getFiler()
            .createResource(
                StandardLocation.CLASS_OUTPUT
                , ""
                , "META-INF/native-image/reflect-config.json"
            );

        try (BufferedWriter writer = new BufferedWriter(reflectionConfig.openWriter()))
        {
            final StringJoiner joiner = new StringJoiner(
                ","
                , "[" + System.lineSeparator()
                , "]"
            );

            benchmarkQNames.stream()
                .map(NativeAssetsGenerator::toReflectionConfigEntry)
                .forEach(joiner::add);
            writer.write(joiner.toString());
        }
    }

    private static String toReflectionConfigEntry(String fqn)
    {
        return "{" + System.lineSeparator()
            + "\"name\":\"" + fqn + "\"," + System.lineSeparator()
            + "\"allDeclaredConstructors\":true," + System.lineSeparator()
            + "\"allDeclaredMethods\":true" + System.lineSeparator()
            + "}" + System.lineSeparator();
    }

    private void findBenchmarkLists() throws IOException, URISyntaxException
    {
        printNote("Find BenchmarkList files");
        final Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/BenchmarkList");

        while (resources.hasMoreElements())
        {
            final URL url = resources.nextElement();
            benchmarkLists.add(url.toURI());
        }
    }

    private void appendBenchmarkList(URI uri, Path toPath) throws IOException
    {
        final URL url = uri.toURL();
        try (final InputStream inputStream = url.openStream()
             ; final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        )
        {
            printNote("Append contents of " + url);
            final byte[] bytes = reader
                .lines()
                .reduce("", (contents, line) -> contents + System.lineSeparator() + line)
                .getBytes(StandardCharsets.UTF_8);
            Files.write(toPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private void findBenchmarksInProject(RoundEnvironment roundEnv)
    {
        printNote("Find generated benchmarks in project");
        for (Element element : roundEnv.getRootElements())
        {
            if (element instanceof TypeElement)
            {
                final TypeElement typeElement = (TypeElement) element;
                final String qualifiedName = typeElement.getQualifiedName().toString();
                if (qualifiedName.contains("jmh_generated"))
                {
                    printNote("Found JMH generated class: " + qualifiedName + " in project");
                    benchmarkQNames.add(qualifiedName);
                }
            }
        }
    }

    private void findBenchmarksInDependencies() throws IOException
    {
        printNote("Find generated benchmarks in dependencies");
        for (URI benchmarkList : benchmarkLists)
        {
            try (final InputStream inputStream = benchmarkList.toURL().openStream()
                 ; final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
            )
            {
                final int benchmarkQNameIndex = 6;
                reader.lines()
                    .map(line -> line.split("\\s+"))
                    .filter(items -> items.length > benchmarkQNameIndex)
                    .map(items -> items[benchmarkQNameIndex]) // position of the benchmark FQN
                    .peek(benchmarkQName ->
                        printNote("Found JMH generated class: " + benchmarkQName + " in dependency")
                    )
                    .forEach(benchmarkQNames::add);
            }
        }
    }

    private void printNote(String msg)
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latest();
    }
}
