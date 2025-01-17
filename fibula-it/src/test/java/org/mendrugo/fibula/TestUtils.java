package org.mendrugo.fibula;

import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

final class TestUtils
{
    static boolean isNativeVm()
    {
        return ForkedVm.instance(
            new MultiVmOutputFormat(new OptionsBuilder().verbosity(VerboseMode.EXTRA).build())
        ).isNativeVm();
    }
}
