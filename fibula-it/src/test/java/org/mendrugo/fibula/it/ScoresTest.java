package org.mendrugo.fibula.it;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScoresTest
{
    static final String MODE = System.getProperty("fibula.test.mode");

    @Test
    public void jmhSample03_States()
    {
        final TestParameters test = TestParameters.parameters("JMHSample_03_States");

        final List<String> arguments = new ArrayList<>(List.of(
            getCurrentJvm()
            , "-jar"
            , "target/bootstrap/quarkus-run.jar"
            , test.benchmarkName()
            , "-rf"
            , "json"
            , "-rff"
            , test.resultWritePath().toString()
        ));

        arguments.addAll(List.of(
            "-r", String.valueOf(test.measurementTime())
            , "-f", String.valueOf(test.measurementForkCount())
            , "-i", String.valueOf(test.measurementIterationCount())
            , "-w", String.valueOf(test.warmupTime())
            , "-wi", String.valueOf(test.warmupIterationCount())
        ));

        TestProcessExecutor.runSync(arguments, test.timeoutMins());
        final List<Result> results = Expects.assertSanityChecks(test);
        System.out.println(results);
    }

    static String getCurrentJvm()
    {
        return System.getProperty("java.home")
            + File.separator
            + "bin"
            + File.separator
            + "java"
            + (isWindows() ? ".exe" : ""
        );
    }

    static boolean isWindows()
    {
        return System.getProperty("os.name").contains("indows");
    }
}
