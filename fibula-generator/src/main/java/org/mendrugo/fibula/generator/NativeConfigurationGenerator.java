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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Generates dynamic native image configuration file based on user defined JMH benchmarks.
 */
@SupportedAnnotationTypes("*")
public class NativeConfigurationGenerator extends AbstractProcessor
{
    File benchmarkListFile;
    final Map<URI, Boolean> benchmarkListInJars = new HashMap<>();
    final List<String> generatedBenchmarkFQNs = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        printNote("New annotation process round");
        processJmhGeneratedInDependencies();
        final boolean newFound = processJmhGeneratedInProject(roundEnv);
        if (!newFound)
        {
            printNote("No new generated JMH benchmarks found");
            if (!generatedBenchmarkFQNs.isEmpty())
            {
                printNote("Write reflection configuration");
                writeReflectionConfiguration();
            }
        }
        return true;
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

                generatedBenchmarkFQNs.stream()
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
                if (benchmarkListFile == null && "file".equals(url.getProtocol()))
                {
                    System.out.println("Found the BenchmarkList file: " + url);
                    benchmarkListFile = new File(url.getFile());
                }
                else if (benchmarkListInJars.putIfAbsent(url.toURI(), false) == null)
                {
                    System.out.println("Found BenchmarkList in dependency: " + url);
                }
            }

            if (benchmarkListFile != null)
            {
                final List<URI> benchmarkListUrlsToProcess = benchmarkListInJars.entrySet().stream()
                    .filter((entry -> !entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

                for (URI uri : benchmarkListUrlsToProcess)
                {
                    appendBenchmarkListTo(uri);
                    benchmarkListInJars.put(uri, true);
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

    private void appendBenchmarkListTo(URI uri) throws IOException
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
            Files.write(benchmarkListFile.toPath(), bytes, StandardOpenOption.APPEND);
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
                    printNote("Found JMH generated class: " + qualifiedName);
                    generatedBenchmarkFQNs.add(qualifiedName);
                    added = true;
                }
            }
        }

        return added;
    }

    private void printNote(String msg)
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        System.out.println(msg);
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latest();
    }
}
