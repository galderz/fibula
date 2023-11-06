package org.mendrugo.fibula.bootstrap;

import java.util.List;

public class PackageTool
{
    List<String> arguments()
    {
        return List.of(
            "mvn"
            , "package"
            , "-DskipTests"
            , "-pl"
            , "fibula-samples"
            , "-Prunner"
            , "-Dquarkus.package.quiltflower.enabled=true" // todo make optional
        );
    }
}
