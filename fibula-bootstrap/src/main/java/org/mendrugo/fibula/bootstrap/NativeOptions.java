package org.mendrugo.fibula.bootstrap;

import io.quarkus.logging.Log;
import org.mendrugo.fibula.results.JmhOptionals;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.Defaults;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.runner.WorkloadParams;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

final class NativeOptions
{
    private final Options jmhOptions;

    NativeOptions(Options jmhOptions)
    {
        this.jmhOptions = jmhOptions;
    }

    SortedSet<BenchmarkParams> findBenchmarkParams(OutputFormat out)
    {
        final String benchmarks = readBenchmarks();
        Log.debugf("Read from benchmark list file: %n%s", benchmarks);

        final BenchmarkList benchmarkList = BenchmarkList.fromString(benchmarks);
        final SortedSet<BenchmarkListEntry> entries = benchmarkList.find(out, jmhOptions.getIncludes(), jmhOptions.getExcludes());
        Log.debugf("Number of benchmarks: %d", entries.size());

        // todo would it work if do stream/map then sort?
        final TreeSet<BenchmarkParams> params = new TreeSet<>();
        for (BenchmarkListEntry entry : entries)
        {
            params.add(getBenchmarkParams(entry));
        }
        return params;
    }

    Optional<String> resultFile()
    {
        if (jmhOptions.getResult().hasValue() || jmhOptions.getResultFormat().hasValue()) {
            final String format = jmhOptions.getResultFormat()
                .orElse(Defaults.RESULT_FORMAT).toString().toLowerCase();
            return Optional.of(jmhOptions.getResult().orElse(Defaults.RESULT_FILE_PREFIX + "." + format));
        }

        return Optional.empty();
    }

    ResultFormat fileResultFormat(String resultFile)
    {
        final ResultFormatType format = jmhOptions.getResultFormat().orElse(Defaults.RESULT_FORMAT);
        return ResultFormatFactory.getInstance(format, resultFile);
    }

    private BenchmarkParams getBenchmarkParams(BenchmarkListEntry benchmark)
    {
        final IterationParams warmup = new IterationParams(
            IterationType.WARMUP
            , getWarmupIterations(benchmark, JmhOptionals.fromJmh(jmhOptions.getWarmupIterations()))
            , getWarmupTime(benchmark, JmhOptionals.fromJmh(jmhOptions.getWarmupTime()))
            , Defaults.WARMUP_BATCHSIZE
        );

        final IterationParams measurement = new IterationParams(
            IterationType.MEASUREMENT
            , getMeasurementIterations(benchmark, JmhOptionals.fromJmh(jmhOptions.getMeasurementIterations()))
            , getMeasurementTime(benchmark, JmhOptionals.fromJmh(jmhOptions.getMeasurementTime()))
            , Defaults.MEASUREMENT_BATCHSIZE
        );
        final WorkloadParams params = new WorkloadParams();

        // Null values fixed at runtime based on vm running fork
        return new BenchmarkParams(
            benchmark.getUsername()
            , benchmark.generatedTarget()
            , true
            , 1
            , new int[]{1}
            , Collections.emptyList()
            , getMeasurementForks(benchmark, JmhOptionals.fromJmh(jmhOptions.getForkCount()))
            , 0
            , warmup
            , measurement
            , benchmark.getMode()
            , params
            , getOutputTimeUnit(benchmark, JmhOptionals.fromJmh(jmhOptions.getTimeUnit()))
            , 1
            , getJvm(benchmark, JmhOptionals.fromJmh(jmhOptions.getJvm()))
            , new ArrayList<>()
            , null
            , null
            , null
            , "fibula-999"
            , TimeValue.minutes(10)
        );
    }

    private static int getMeasurementForks(BenchmarkListEntry benchmark, Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getForks()
            .orElse(Defaults.MEASUREMENT_FORKS));
    }

    private static int getMeasurementIterations(BenchmarkListEntry benchmark, java.util.Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getMeasurementIterations()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? Defaults.MEASUREMENT_ITERATIONS_SINGLESHOT
                : Defaults.MEASUREMENT_ITERATIONS)
            );
    }

    private static TimeValue getMeasurementTime(BenchmarkListEntry benchmark, Optional<TimeValue> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getMeasurementTime()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? TimeValue.NONE
                : Defaults.MEASUREMENT_TIME)
            );
    }

    private static TimeUnit getOutputTimeUnit(BenchmarkListEntry benchmark, Optional<TimeUnit> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getTimeUnit()
                .orElse(Defaults.OUTPUT_TIMEUNIT)
            );
    }

    private static int getWarmupIterations(BenchmarkListEntry benchmark, Optional<Integer> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getWarmupIterations()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? Defaults.WARMUP_ITERATIONS_SINGLESHOT
                : Defaults.WARMUP_ITERATIONS)
            );
    }

    private static TimeValue getWarmupTime(BenchmarkListEntry benchmark, Optional<TimeValue> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getWarmupTime()
            .orElse(benchmark.getMode() == Mode.SingleShotTime
                ? TimeValue.NONE
                : Defaults.WARMUP_TIME)
            );
    }

    private static String getJvm(BenchmarkListEntry benchmark, Optional<String> cmdLineValue)
    {
        return cmdLineValue
            .orElse(benchmark.getJvm()
            .orElse(Utils.getCurrentJvm()));
    }

    private String readBenchmarks()
    {
        final File resourceDir = Path.of("target", "classes").toFile();
        final FileSystemDestination destination = new FileSystemDestination(resourceDir, null);
        try (InputStream stream = destination.getResource(BenchmarkList.BENCHMARK_LIST.substring(1)))
        {
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                final Collection<String> lines = FileUtils.readAllLines(reader);
                return String.join(System.lineSeparator(), lines);
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
        return "";
    }
}
