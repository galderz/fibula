package org.mendrugo.fibula.it;

import org.openjdk.jmh.runner.Defaults;

public class SpeedSetup
{
    // static final boolean QUICK = Boolean.getBoolean("fibula.test.quick");
    static final boolean QUICK = true;

    static final int QUICK_TIMEOUT = 1;
    static final String QUICK_MEASUREMENT_TIME = "1 s";
    static final int QUICK_MEASUREMENT_FORKS = 1;
    static final int QUICK_MEASUREMENT_ITERATIONS = 1;
    static final String QUICK_WARMUP_TIME = "1 s";
    static final int QUICK_WARMUP_ITERATIONS = 1;

    static SpeedParameters parameters()
    {
        if (QUICK)
        {
            return new SpeedParameters(
                QUICK_TIMEOUT
                , QUICK_MEASUREMENT_TIME
                , QUICK_MEASUREMENT_FORKS
                , QUICK_MEASUREMENT_ITERATIONS
                , QUICK_WARMUP_TIME
                , QUICK_WARMUP_ITERATIONS
            );
        }

        return new SpeedParameters(
            20
            , Defaults.MEASUREMENT_TIME.toString()
            , Defaults.MEASUREMENT_FORKS
            , Defaults.MEASUREMENT_ITERATIONS
            , Defaults.WARMUP_TIME.toString()
            , Defaults.WARMUP_ITERATIONS
        );
    }
}
