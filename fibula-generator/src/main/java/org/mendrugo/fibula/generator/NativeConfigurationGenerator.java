package org.mendrugo.fibula.generator;

import org.mendrugo.fibula.GraalBlackhole;

import javax.annotation.processing.AbstractProcessor;
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
 * Generates dynamic native image configuration file based on user defined JMH benchmarks.
 */
@SupportedAnnotationTypes("*")
public class NativeConfigurationGenerator extends AbstractProcessor
{
    // Set of BenchmarkList files in dependencies
    final Set<URI> benchmarkLists = new HashSet<>();
    final List<String> benchmarkQNames = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        try
        {
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

        final Path classOutputPath = Paths.get(ignore.toUri()).getParent();
        return classOutputPath;
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
                .map(NativeConfigurationGenerator::toReflectionConfigEntry)
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
            Files.write(toPath, bytes, StandardOpenOption.APPEND);
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
