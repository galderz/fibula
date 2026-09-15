package org.mendrugo.fibula;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.Aggregator;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.runner.IterationType;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NativeAsyncProfiler implements ExternalProfiler, InternalProfiler
{
    private int measurementIterationCount;

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params)
    {
        return List.of(
            "LD_PRELOAD=/home/agentuser/opt/async-profiler/lib/libasyncProfiler.so ASPROF_COMMAND=start,event=cpu,file=profile.jfr"
        );
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params)
    {
        return Collections.emptyList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams)
    {
        // Do nothing
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr)
    {
        List<FileResult> moved = new ArrayList<>();
        for (String label : Arrays.asList("async-summary", "async-collapsed", "async-flamegraph", "async-tree", "async-jfr")) {
            FileResult result = (FileResult) br.getSecondaryResults().remove(label);
            if (result != null) {
                moved.add(new FileResult(result.getLabel(), result.files.stream()
                    .flatMap(f -> Stream.of(f, addDiscriminator(f, pid)))
                    .collect(Collectors.toList())));
            }
        }
        return moved;
    }

    private File addDiscriminator(File original, long pid) {
        String originalName = original.getPath();
        int extIndex = originalName.lastIndexOf('.');
        File newFile = new File(originalName.substring(0, extIndex) + "." + pid + originalName.substring(extIndex));
        try {
            Files.copy(original.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return newFile;
    }

    @Override
    public boolean allowPrintOut()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public boolean allowPrintErr()
    {
        return false;  // TODO: Customise this generated block
    }

    @Override
    public String getDescription()
    {
        return "";  // TODO: Customise this generated block
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams)
    {
        // TODO: Customise this generated block
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result)
    {
        List<Result<?>> results = new ArrayList<>();
        if (iterationParams.getType() == IterationType.MEASUREMENT) {
            measurementIterationCount += 1;
            if (measurementIterationCount == iterationParams.getCount()) {
                results.add(new FileResult("async-jfr", Collections.singletonList(new File("profile.jfr"))));
                return results;
            }
        }

        return Collections.emptyList();
    }


    public final static class FileResult extends Result<FileResult> {
        private final List<File> files;

        FileResult(String label, List<File> files) {
            super(ResultRole.SECONDARY, label, of(Double.NaN), "---", AggregationPolicy.AVG);
            this.files = files;
        }

        @Override
        protected Aggregator<FileResult> getThreadAggregator() {
            return new FileAggregator();
        }

        @Override
        protected Aggregator<FileResult> getIterationAggregator() {
            return new FileAggregator();
        }

        public Collection<? extends File> getFiles() {
            return files;
        }

        @Override
        public String toString() {
            return "Files: " + files;
        }

        @Override
        public String extendedInfo() {
            StringBuilder builder = new StringBuilder("Async profiler results:").append(System.lineSeparator());
            for (File file : files) {
                builder.append("  ").append(file.getPath()).append(System.lineSeparator());
            }
            return builder.toString();
        }

        private static class FileAggregator implements Aggregator<FileResult> {
            @Override
            public FileResult aggregate(Collection<FileResult> results) {
                return new FileResult(results.iterator().next().getLabel(), results.stream()
                    .flatMap(r -> r.files.stream())
                    .distinct()
                    .collect(Collectors.toList()));
            }
        }
    }
}
