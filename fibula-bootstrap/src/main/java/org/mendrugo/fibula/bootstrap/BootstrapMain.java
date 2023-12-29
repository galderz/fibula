package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.JmhOptionals;
import org.mendrugo.fibula.results.NativeBenchmarkParams;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain implements QuarkusApplication
{
    @Inject
    ResultService resultService;

    @Override
    public int run(String... args) throws Exception
    {
        System.out.println("Running bootstrap main...");

        // Read command line arguments just like JMH does
        final Options jmhOptions = new CommandLineOptions(args);
        final NativeOptions options = new NativeOptions(jmhOptions);
        resultService.setOptions(options);

        final ProcessRunner processRunner = new ProcessRunner(options);
        resultService.setProcessRunner(processRunner);

        // Build the runner and run the first fork
        processRunner.runBuild();

        // Read metadata for all benchmarks
        final Set<NativeBenchmarkParams> benchmarks = readBenchmarks();
        final NativeBenchmarkParams benchmark = benchmarks.iterator().next();

//        final OutputFormat out = JmhFormats.outputFormat();
        // todo call out.startBenchmark()

        final int forks = benchmark.getMeasurementForks(JmhOptionals.fromJmh(options.getMeasurementForks()));
        processRunner.runFork(1, forks);

        Quarkus.waitForExit();
        return 0;
    }

    private Set<NativeBenchmarkParams> readBenchmarks()
    {
        // todo avoid hardcoding sample project name
        final File resourceDir = Path.of("fibula-samples", "target", "classes").toFile();
        final File sourceDir = Path.of("fibula-samples", "target", "classes", "tbd").toFile();
        final FileSystemDestination destination = new FileSystemDestination(resourceDir, sourceDir);
        try (InputStream stream = destination.getResource(BenchmarkList.BENCHMARK_LIST.substring(1)))
        {
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                return FileUtils.readAllLines(reader).stream()
                    .map(NativeBenchmarkParams::new)
                    .collect(Collectors.toUnmodifiableSet());
            }
        }
        catch (IOException e)
        {
            Log.debug("Unable to read benchmark list", e);
        }
        catch (UnsupportedOperationException e)
        {
            final String msg = "Unable to read the existing benchmark list.";
            Log.debug(msg, e);
            destination.printError(msg, e);
        }
        return Collections.emptySet();
    }
}
