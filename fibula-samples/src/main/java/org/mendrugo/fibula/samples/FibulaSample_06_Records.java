package org.mendrugo.fibula.samples;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.Objects;

public class FibulaSample_06_Records
{
    @State(Scope.Thread)
    public static class BenchmarkState
    {
        PositionAsRecord posRec1 = new PositionAsRecord(0.0, 1.0, 2.0, 3.0f, 4.0f);
        PositionAsRecord posRec1Copy = new PositionAsRecord(0.0, 1.0, 2.0, 3.0f, 4.0f);

        PositionHandrolled posHand1 = new PositionHandrolled(0.0, 1.0, 2.0, 3.0f, 4.0f);
        PositionHandrolled posHand1Copy = new PositionHandrolled(0.0, 1.0, 2.0, 3.0f, 4.0f);
    }

    @Benchmark
    public boolean equalsPositionAsRecord(BenchmarkState state)
    {
        return state.posRec1.equals(state.posRec1Copy);
    }

    @Benchmark
    public int hashcodePositionAsRecord(BenchmarkState state)
    {
        return state.posRec1.hashCode();
    }

    @Benchmark
    public boolean equalsPositionHandrolled(BenchmarkState state)
    {
        return state.posHand1.equals(state.posHand1Copy);
    }

    @Benchmark
    public int hashcodePositionHandrolled(BenchmarkState state)
    {
        return state.posHand1.hashCode();
    }

    public record PositionAsRecord(double x, double y, double z, float yaw, float pitch) {}

    public static final class PositionHandrolled
    {
        final double x;
        final double y;
        final double z;
        final float yaw;
        final float pitch;

        public PositionHandrolled(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PositionHandrolled that = (PositionHandrolled) o;
            return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && Double.compare(z, that.z) == 0 && Float.compare(yaw, that.yaw) == 0 && Float.compare(pitch, that.pitch) == 0;
        }

        @Override
        public int hashCode()
        {
            int result = 1;
            result = 31 * result + Double.hashCode(x);
            result = 31 * result + Double.hashCode(y);
            result = 31 * result + Double.hashCode(z);
            result = 31 * result + Float.hashCode(yaw);
            result = 31 * result + Float.hashCode(pitch);
            return result;
        }
    }
}
