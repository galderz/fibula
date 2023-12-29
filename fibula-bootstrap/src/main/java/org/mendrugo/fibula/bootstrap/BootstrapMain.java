package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.mendrugo.fibula.results.JmhFormats;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

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

        readBenchmarkList();
//
//        final OutputFormat out = JmhFormats.outputFormat();
        processRunner.runFork(1);

        Quarkus.waitForExit();
        return 0;
    }

    private void readBenchmarkList()
    {
        // todo avoid hardcoding sample project name
        final File resourceDir = Path.of("fibula-samples", "target", "classes").toFile();
        final File sourceDir = Path.of("fibula-samples", "target", "classes", "tbd").toFile();
        final FileSystemDestination destination = new FileSystemDestination(resourceDir, sourceDir);
        try (InputStream stream = destination.getResource(BenchmarkList.BENCHMARK_LIST.substring(1))) {
            for (BenchmarkListEntry ble : BenchmarkList.readBenchmarkList(stream)) {
                System.out.println(ble);
//                entries.add(ble);
//                entriesByQName.put(ble.getUserClassQName(), ble);
            }
        } catch (IOException e) {
            // okay, move on
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            destination.printError("Unable to read the existing benchmark list.", e);
        }
    }
}
