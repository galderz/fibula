package org.mendrugo.fibula.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class Benchmark
{
    static List<Result> run(String benchmarkName, Provider provider)
    {
        final Parameters test = Parameters.parameters(benchmarkName);

        final List<String> arguments = new ArrayList<>(List.of(
            getCurrentJvm()
            , "-jar"
            , provider.jarPath().toString()
            , test.benchmarkName()
            , "-rf"
            , "json"
            , "-rff"
            , test.resultPath().toString()
        ));

        arguments.addAll(List.of(
            "-r", String.valueOf(test.measurementTime())
            , "-f", String.valueOf(test.measurementForkCount())
            , "-i", String.valueOf(test.measurementIterationCount())
            , "-w", String.valueOf(test.warmupTime())
            , "-wi", String.valueOf(test.warmupIterationCount())
        ));

        // todo return working directoy and use that for reading the file (avoids a method)
        ProcessExecutor.runSync(arguments, test);
        return Expects.assertSanityChecks(test);
    }

    private static String getCurrentJvm()
    {
        return System.getProperty("java.home")
            + File.separator
            + "bin"
            + File.separator
            + "java"
            + (isWindows() ? ".exe" : ""
        );
    }

    private static boolean isWindows()
    {
        return System.getProperty("os.name").contains("indows");
    }

    private Benchmark()
    {
    }
}
