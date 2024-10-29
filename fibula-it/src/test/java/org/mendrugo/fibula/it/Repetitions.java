package org.mendrugo.fibula.it;

import java.security.AccessController;
import java.security.PrivilegedAction;

public final class Repetitions
{
    private static final int REPS;

    static
    {
        REPS = AccessController.doPrivileged(
            (PrivilegedAction<Integer>) () -> Integer.getInteger("jmh.it.reps", 1)
        );
    }

    public static int count() {
        return REPS;
    }

    private Repetitions() {}
}
