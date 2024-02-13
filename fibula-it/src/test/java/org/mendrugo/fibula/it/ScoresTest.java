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
        System.out.println("Working Directory: " + System.getProperty("user.dir"));

        final List<String> arguments = new ArrayList<>(List.of(
            getCurrentJvm()
            , "-jar"
            , "target/bootstrap/quarkus-run.jar"
            , "JMHSample_03_States"
            , "-rf"
            , "json"
            , "-rff"
            , "target/fibula_JmhSample03_States.json"
        ));

        final SpeedParameters speedParameters = SpeedSetup.parameters();
        arguments.addAll(List.of(
            "-r", String.valueOf(speedParameters.measureTime())
            , "-f", String.valueOf(speedParameters.measureForkCount())
            , "-i", String.valueOf(speedParameters.measureIterationCount())
            , "-w", String.valueOf(speedParameters.warmupTime())
            , "-wi", String.valueOf(speedParameters.warmupIterationCount())
        ));

        TestProcessExecutor.runSync(arguments, speedParameters.timeoutMins());
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
