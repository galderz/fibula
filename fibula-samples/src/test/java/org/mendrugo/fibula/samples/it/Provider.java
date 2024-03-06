package org.mendrugo.fibula.samples.it;

import java.nio.file.Path;

record Provider(
    Path workingDir
    , Path jarPath
)
{
    static final Provider FIBULA = new Provider(
        Path.of(".")
        , Path.of("target", "benchmarks.jar")
    );

    static final Provider JMH = new Provider(
        Path.of("..", "..", "jmh", "jmh-samples")
        , Path.of("target", "benchmarks.jar")
    );
}
