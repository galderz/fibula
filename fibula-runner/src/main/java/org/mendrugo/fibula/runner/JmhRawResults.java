package org.mendrugo.fibula.runner;

import org.openjdk.jmh.results.RawResults;

/**
 * Raw results helper methods.
 * The logic is simple enough that it could potentially be written in Gizmo,
 * but it lacks support for primitive conversions, e.g. l2d,
 * so the helper is really needed.
 */
@SuppressWarnings("unused")
public final class JmhRawResults
{
    public static void setMeasuredOps(long operations, RawResults raw)
    {
        raw.measuredOps = operations;
    }

    private JmhRawResults()
    {
    }
}
