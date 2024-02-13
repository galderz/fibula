package org.mendrugo.fibula.it;

import java.nio.file.Path;

record Provider(
    Path jarPath
)
{
    static final Provider FIBULA = new Provider(
        Path.of("target", "bootstrap", "quarkus-run.jar")
    );
}
