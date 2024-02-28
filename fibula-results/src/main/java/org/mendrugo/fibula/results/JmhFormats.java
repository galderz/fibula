package org.mendrugo.fibula.results;

import org.openjdk.jmh.results.format.ResultFormat;
import org.openjdk.jmh.results.format.ResultFormatFactory;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.openjdk.jmh.util.UnCloseablePrintStream;
import org.openjdk.jmh.util.Utils;

import java.io.UnsupportedEncodingException;

// todo move to bootstrap and make take options and other info into account
public final class JmhFormats
{
    public static OutputFormat outputFormat()
    {
        try
        {
            final UnCloseablePrintStream printStream = printStream();
            return OutputFormatFactory.createFormatInstance(printStream, VerboseMode.NORMAL);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public static ResultFormat textResultFormat()
    {
        try
        {
            final UnCloseablePrintStream printStream = printStream();
            return ResultFormatFactory.getInstance(ResultFormatType.TEXT, printStream);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static UnCloseablePrintStream printStream() throws UnsupportedEncodingException
    {
        return new UnCloseablePrintStream(System.out, Utils.guessConsoleEncoding());
    }

    private JmhFormats()
    {
    }
}
