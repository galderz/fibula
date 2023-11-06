package org.mendrugo.fibula.bootstrap;

public class ScoreFormatter
{
    private static final int PRECISION = Integer.getInteger("jmh.scorePrecision", 3);
    private static final double ULP = 1.0 / Math.pow(10, PRECISION);
    private static final double THRESHOLD = ULP / 2;

    public static boolean isApproximate(double score)
    {
        return (score < THRESHOLD);
    }

    public static String format(double score)
    {
        if (isApproximate(score))
        {
            int power = (int) Math.round(Math.log10(score));
            return "\u2248 " + ((power != 0) ? "10" + superscript("" + power) : "0");
        }
        else
        {
            return String.format("%." + PRECISION + "f", score);
        }
    }

    public static String format(int width, double score)
    {
        if (isApproximate(score))
        {
            int power = (int) Math.round(Math.log10(score));
            return String.format("%" + width + "s", "\u2248 " + ((power != 0) ? "10" + superscript("" + power) : "0"));
        }
        else
        {
            return String.format("%" + width + "." + PRECISION + "f", score);
        }
    }

    public static String superscript(String str)
    {
        str = str.replaceAll("-", "\u207b");
        str = str.replaceAll("0", "\u2070");
        str = str.replaceAll("1", "\u00b9");
        str = str.replaceAll("2", "\u00b2");
        str = str.replaceAll("3", "\u00b3");
        str = str.replaceAll("4", "\u2074");
        str = str.replaceAll("5", "\u2075");
        str = str.replaceAll("6", "\u2076");
        str = str.replaceAll("7", "\u2077");
        str = str.replaceAll("8", "\u2078");
        str = str.replaceAll("9", "\u2079");
        return str;
    }
}
