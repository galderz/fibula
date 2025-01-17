package org.mendrugo.fibula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PgoArgumentsTest
{
    @Test
    public void shouldAddWarmupForksWhenNoWarmupForksInArgs()
    {
        String[] processed = Pgo.ENABLED.preProcessArgs(new String[]{});
        assertEquals(2, processed.length);
        assertEquals("-wf", processed[0]);
        assertEquals("1", processed[1]);

        processed = Pgo.ENABLED.preProcessArgs(new String[]{"-f", "1"});
        assertEquals(4, processed.length);
        assertEquals("-wf", processed[2]);
        assertEquals("1", processed[3]);
    }

    @Test
    public void shouldNotAddWarmupForksWhenWarmupForksInArgs()
    {
        String[] processed = Pgo.ENABLED.preProcessArgs(new String[]{"-wf", "1"});
        assertEquals(2, processed.length);
        assertEquals("-wf", processed[0]);
        assertEquals("1", processed[1]);

        processed = Pgo.ENABLED.preProcessArgs(new String[]{"-f", "1", "-wf", "2"});
        assertEquals(4, processed.length);
        assertEquals("-wf", processed[2]);
        assertEquals("2", processed[3]);
    }

    @Test
    public void shouldFailWhenWarmupForksAreZero()
    {
        assertThrows(
            IllegalArgumentException.class
            , () -> Pgo.ENABLED.preProcessArgs(new String[]{"-wf", "0"})
        );
    }
}
