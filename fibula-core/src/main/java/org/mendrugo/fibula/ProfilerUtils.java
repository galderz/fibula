package org.mendrugo.fibula;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.BenchmarkResultMetaData;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

class ProfilerUtils
{
    public static OptionSet parseInitLine(String initLine, OptionParser parser) throws ProfilerException
    {
        parser.accepts("help", "Display help.");

        OptionSpec<String> nonOptions = parser.nonOptions();

        String[] split = initLine.split(";");
        for (int c = 0; c < split.length; c++)
        {
            if (!split[c].isEmpty())
            {
                split[c] = "-" + split[c];
            }
        }

        OptionSet set;
        try
        {
            set = parser.parse(split);
        }
        catch (OptionException e)
        {
            try
            {
                StringWriter sw = new StringWriter();
                sw.append(e.getMessage());
                sw.append("\n");
                parser.printHelpOn(sw);
                throw new ProfilerException(sw.toString());
            }
            catch (IOException e1)
            {
                throw new ProfilerException(e1);
            }
        }

        if (set.has("help"))
        {
            try
            {
                StringWriter sw = new StringWriter();
                parser.printHelpOn(sw);
                throw new ProfilerException(sw.toString());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        String s = set.valueOf(nonOptions);
        if (s != null && !s.isEmpty())
        {
            throw new ProfilerException("Unhandled options: " + s + " in " + initLine);
        }
        return set;
    }

    public static long measurementDelayMs(BenchmarkResult br)
    {
        BenchmarkResultMetaData md = br.getMetadata();
        if (md != null)
        {
            // try to ask harness itself:
            return md.getMeasurementTime() - md.getStartTime();
        } else
        {
            // metadata is not available, let's make a guess:
            IterationParams wp = br.getParams().getWarmup();
            return wp.getCount() * wp.getTime().convertTo(TimeUnit.MILLISECONDS) + TimeUnit.SECONDS.toMillis(1); // loosely account for the JVM lag
        }
    }

    public static long measuredTimeMs(BenchmarkResult br)
    {
        BenchmarkResultMetaData md = br.getMetadata();
        if (md != null)
        {
            // try to ask harness itself:
            return md.getStopTime() - md.getMeasurementTime();
        } else
        {
            // metadata is not available, let's make a guess:
            IterationParams mp = br.getParams().getMeasurement();
            return mp.getCount() * mp.getTime().convertTo(TimeUnit.MILLISECONDS);
        }
    }
}
