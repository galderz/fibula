package org.mendrugo.fibula.generator;

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
    final List<String> generatedBenchmarks = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        printNote("New annotation process round");
        if (!roundEnv.processingOver())
        {
            printNote("Discover generated JMH benchmarks");
            processJmhGeneratedInDependencies();
            processJmhGeneratedInProject(roundEnv);
        }
        else
        {
            printNote("Write reflection configuration");
            writeReflectionConfiguration();
            writeBenchmarkList();
        }
        return true;
    }

    private void writeBenchmarkList()
    {
        try
        {
            final FileObject ignore = processingEnv.getFiler()
                .createResource(
                    StandardLocation.CLASS_OUTPUT
                    , ""
                    , "ignore"
                );
            // System.out.println(ignore.toUri().getPath());
            // System.out.println();
            final Path classOutputPath = Paths.get(ignore.toUri()).getParent();

//            final FileObject benchmarkList = processingEnv.getFiler()
//                .getResource(
//                    StandardLocation.CLASS_OUTPUT
//                    , ""
//                    , "META-INF/BenchmarkList"
//                );
//            Path benchmarkListPath = benchmarkList.toUri().getPath();

            Path benchmarkListPath = classOutputPath.resolve("META-INF/BenchmarkList");

            for (URI uri : benchmarkLists)
            {
                appendBenchmarkList(uri, benchmarkListPath);
                // benchmarkListInJars.put(uri, true);
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void writeReflectionConfiguration()
    {
        try
        {
            final FileObject reflectionConfig = processingEnv.getFiler()
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

                generatedBenchmarks.stream()
                    .map(NativeConfigurationGenerator::toReflectionConfigEntry)
                    .forEach(joiner::add);
                writer.write(joiner.toString());
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
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

    // todo make the method a find and make it have a sole responsibility
    // todo move appending generated benchmarks elsewhere
    private void processJmhGeneratedInDependencies()
    {
        try
        {
            final Enumeration<URL> resources = getClass()
                .getClassLoader()
                .getResources("META-INF/BenchmarkList");

            while (resources.hasMoreElements())
            {
                final URL url = resources.nextElement();
                if (benchmarkLists.add(url.toURI()))
                {
                    printNote("Found BenchmarkList in dependency: " + url);
                    appendGeneratedBenchmarks(url.toURI());
                }
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
    }

    private void appendGeneratedBenchmarks(URI uri) throws IOException
    {
        final URL url = uri.toURL();
        try (final InputStream inputStream = url.openStream();
             final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        )
        {
            final int benchmarkFqnIndex = 6;
            reader.lines()
                .map(line -> line.split("\\s+"))
                .filter(items -> items.length > benchmarkFqnIndex)
                //.peek(items -> System.out.println(items.length))
                .map(items -> items[benchmarkFqnIndex]) // position of the benchmark FQN
                //.peek(System.out::println)
                // todo use peek instead of adding printNote to forEach
                .forEach(benchmark -> {
                    printNote("Found JMH generated class: " + benchmark + " in dependency");
                    generatedBenchmarks.add(benchmark);
                });
        }
    }

    private void appendBenchmarkList(URI uri, Path toPath) throws IOException
    {
        final URL url = uri.toURL();
        try (final InputStream inputStream = url.openStream();
             final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        )
        {
            printNote("Append contents of " + url);
            final byte[] bytes = reader.lines()
                .reduce("", (contents, line) -> contents + System.lineSeparator() + line)
                .getBytes(StandardCharsets.UTF_8);
            Files.write(toPath, bytes, StandardOpenOption.APPEND);
        }
    }

    private boolean processJmhGeneratedInProject(RoundEnvironment roundEnv)
    {
        boolean added = false;
        for (Element element : roundEnv.getRootElements())
        {
            if (element instanceof TypeElement)
            {
                final TypeElement typeElement = (TypeElement) element;
                final String qualifiedName = typeElement.getQualifiedName().toString();
                if (qualifiedName.contains("jmh_generated"))
                {
                    printNote("Found JMH generated class: " + qualifiedName + " in project");
                    generatedBenchmarks.add(qualifiedName);
                    added = true;
                }
            }
        }

        return added;
    }

    private void printNote(String msg)
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        // System.out.println(msg);
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latest();
    }
}
