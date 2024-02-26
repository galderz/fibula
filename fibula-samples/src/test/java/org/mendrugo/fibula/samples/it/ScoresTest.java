package org.mendrugo.fibula.samples.it;

import org.junit.jupiter.api.Test;

public class ScoresTest
{
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
    public void jmhSample09_Blackholes()
    {
        Expects.expectScoresNearEqual("JMHSample_09_Blackholes");
    }
}
