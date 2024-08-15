package org.mendrugo.fibula.bootstrap;

import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.UnCloseablePrintStream;
import org.openjdk.jmh.util.Utils;

import java.io.UnsupportedEncodingException;

public class OutputFormats
{
    public static OutputFormat outputFormat()
    {
        try
        {
            final UnCloseablePrintStream printStream = new UnCloseablePrintStream(System.out, Utils.guessConsoleEncoding());
            return OutputFormatFactory.createFormatInstance(printStream, VerboseMode.NORMAL);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
