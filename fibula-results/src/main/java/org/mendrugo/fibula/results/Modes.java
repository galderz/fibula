package org.mendrugo.fibula.results;

import org.openjdk.jmh.annotations.Mode;

import java.util.Arrays;
import java.util.stream.Stream;

public final class Modes
{
    public static Stream<Mode> nonAll()
    {
        // todo support remaining modes
        return Arrays.stream(Mode.values())
            .filter(mode -> mode == Mode.Throughput || mode == Mode.AverageTime);
            // .filter(mode -> mode != Mode.All);
    }
    
    private Modes()
    {
    }
}
