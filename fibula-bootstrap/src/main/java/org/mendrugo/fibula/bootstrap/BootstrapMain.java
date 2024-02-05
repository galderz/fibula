package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.format.OutputFormat;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

@QuarkusMain(name = "bootstrap")
public class BootstrapMain implements QuarkusApplication
{
    @Inject
    ResultService resultService;

    @Override
    public int run(String... args) throws Exception
    {
        Log.debug("Running bootstrap");

        // Read command line arguments just like JMH does
        final Options jmhOptions = new CommandLineOptions(args);
        final NativeOptions options = new NativeOptions(jmhOptions);

        final OutputFormat out = JmhFormats.outputFormat();
        final ProcessRunner processRunner = new ProcessRunner(out);

        resultService.startRun(options);

        // Read metadata for all benchmarks
        final SortedSet<BenchmarkParams> benchmarks = options.findBenchmarkParams(out);
        for (BenchmarkParams benchmark : benchmarks)
        {
            out.startBenchmark(benchmark);
            out.println("");

            final int forkCount = benchmark.getMeasurement().getCount();
            for (int i = 0; i < forkCount; i++)
            {
                final Process process = processRunner.runFork(i + 1, benchmark);
                final int exitCode = process.waitFor();
                if (exitCode != 0)
                {
                    throw new RuntimeException(String.format(
                        "Error in forked runner (exit code %d)"
                        , exitCode
                    ));
                }
            }

            resultService.endBenchmark(benchmark, out);
        }

        resultService.endRun();
        return 0;
    }
}
