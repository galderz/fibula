package org.mendrugo.fibula.it;

import org.junit.jupiter.api.Test;

public class ScoresTest
{
    static final String MODE = System.getProperty("fibula.test.mode");

    @Test
    public void jmhSample03_States()
    {
        Expects.expectScoresNearEqual("JMHSample_03_States");
    }
}
