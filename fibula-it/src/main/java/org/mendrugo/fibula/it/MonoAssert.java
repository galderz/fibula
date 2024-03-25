package org.mendrugo.fibula.it;

import static org.hamcrest.MatcherAssert.assertThat;

final class MonoAssert
{
    static void assertThatGreaterThan(int actual, int expected)
    {
        String errorMsg = """
            %nExpected: a value greater than %1$d
                 but: %2$d was less than (or equal to) %1$d
            """.formatted(expected, actual);
        assertThat(errorMsg, actual > expected);
    }
}
