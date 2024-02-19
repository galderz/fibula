package org.mendrugo.fibula.it;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ScoresTest
{
    static final String MODE = System.getProperty("fibula.test.mode");

    @Test
    public void jmhSample03_States()
    {
        Expects.expectScoresNearEqual("JMHSample_03_States");
    }

    @Test
    public void jmhSample04_DefaultState()
    {
        Expects.expectScoresNearEqual("JMHSample_04_DefaultState");
    }

    @Test
    @Disabled("Difference with HotSpot is > 10%, needs further investigation")
    public void jmhSample09_Blackholes()
    {
        Expects.expectScoresNearEqual("JMHSample_09_Blackholes");
    }
}
