package org.mendrugo.fibula.bootstrap;

import java.util.ArrayList;
import java.util.List;

final class PackageTool
{
    List<String> buildArguments(PackageMode packageMode)
    {
        return switch (packageMode)
        {
            case JVM ->
                List.of(
                    "mvn"
                    , "package"
                    , "-DskipTests"
                    , "-pl"
                    , "fibula-samples"
                    , "-Prunner"
                    , "-Dquarkus.package.quiltflower.enabled=true" // todo make optional
                );
            case NATIVE ->
                List.of(
                    "mvn"
                    , "package"
                    , "-DskipTests"
                    , "-pl"
                    , "fibula-samples"
                    , "-Prunner-native"
                    , "-Dquarkus.package.quiltflower.enabled=true" // todo make optional
                );
        };
    }

    List<String> runArguments(PackageMode packageMode, List<String> runnerArgs)
    {
        final List<String> runArguments = new ArrayList<>();
        final List<String> baseArguments = switch (packageMode)
        {
            case JVM -> List.of(
                "java"
                , "-jar"
                , "fibula-samples/target/runner-app/quarkus-run.jar"
            );
            case NATIVE -> List.of(
                "./fibula-samples/target/runner-app/fibula-samples-1.0.0-SNAPSHOT-runner"
            );
        };

        runArguments.addAll(baseArguments);
        runArguments.addAll(runnerArgs);
        return runArguments;
    }
}
