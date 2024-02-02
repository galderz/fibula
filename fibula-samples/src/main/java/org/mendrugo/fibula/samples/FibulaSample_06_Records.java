package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class FibulaSample_06_Records
{
    @State(Scope.Thread)
    public static class BenchmarkState
    {
        Pos pos1 = new Pos(0.0, 1.0, 2.0, 3.0f, 4.0f);
        Pos pos1Copy = new Pos(0.0, 1.0, 2.0, 3.0f, 4.0f);
    }

    @Benchmark
    public boolean equalsPositions(BenchmarkState state)
    {
        return state.pos1.equals(state.pos1Copy);
    }

    @Benchmark
    public int hashcodePosition(BenchmarkState state)
    {
        return state.pos1.hashCode();
    }

    public record Pos(double x, double y, double z, float yaw, float pitch) {}
}
