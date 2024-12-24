package org.mendrugo.fibula;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

enum Pgo
{
    // todo just leave instance instead of ENABLE/DISABLED

    ENABLED(
        Path.of("target", "benchmarks.nib").toFile()
        , Path.of("target", "benchmarks-optimized.nib").toFile()
    )
    , DISABLED(
        new File("NOT_FOUND")
        , new File("NOT_FOUND")
    );

    final File bundle;
    final File bundleOptimized;

    Pgo(File bundle, File bundleOptimized)
    {
        this.bundle = bundle;
        this.bundleOptimized = bundleOptimized;
    }

    boolean isEnabled()
    {
        return this == ENABLED;
    }

    static Pgo instance()
    {
        return Pgo.ENABLED.bundle.exists()
            ? Pgo.ENABLED
            : Pgo.DISABLED;
    }

    private static boolean existsPgoBundle()
    {
        return Path.of("target", "benchmarks.nib")
            .toFile()
            .exists();
    }

    // Pre-process arguments
    String[] preProcessArgs(String[] args)
    {
        return switch (this)
        {
            case ENABLED -> appendInstrumentArgs(args);
            case DISABLED -> args;
        };
    }

    private static String[] appendInstrumentArgs(String[] args)
    {
        final List<String> instrumentArgs = new ArrayList<>(Arrays.asList(args));
        // Add a warmup fork artificially.
        // This fork will be used to run the benchmark and instrument it.
        // Once finished the native binary will be rebuilt with the instrumentation for subsequent forks.
        instrumentArgs.add("-wf");
        instrumentArgs.add("1");
        instrumentArgs.toArray(new String[0]);
        return instrumentArgs.toArray(new String[0]);
    }
}
