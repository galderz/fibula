package org.mendrugo.fibula;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class GraalVersionParser
{
    // Java version info (suitable for Runtime.Version.parse()). See java.lang.VersionProps
    private static final String VNUM = "(?<VNUM>[1-9][0-9]*(?:(?:\\.0)*\\.[1-9][0-9]*)*)";
    private static final String PRE = "(?:-(?<PRE>[a-zA-Z0-9]+))?";
    private static final String BUILD = "(?:(?<PLUS>\\+)(?<BUILD>0|[1-9][0-9]*)?)?";
    private static final String OPT = "(?:-(?<OPT>[-a-zA-Z0-9.]+))?";
    private static final String VSTR_FORMAT = VNUM + PRE + BUILD + OPT;

    private static final String BUILD_INFO_GROUP = "BUILDINFO";

    private static final String VENDOR_VERS = "(?<VENDOR>.*)";
    private static final String JDK_DEBUG = "[^\\)]*"; // zero or more of >anything not a ')'<
    private static final String RUNTIME_NAME = "(?<RUNTIME>(?:.*) Runtime Environment) ";
    private static final String BUILD_INFO = "(?<BUILDINFO>.*)";
    private static final String VM_NAME = "(?<VM>(?:.*) VM) ";

    private static final String FIRST_LINE_PATTERN = "native-image " + VSTR_FORMAT + " .*$";
    private static final String SECOND_LINE_PATTERN = RUNTIME_NAME + VENDOR_VERS + " \\(" + JDK_DEBUG + "build " + BUILD_INFO + "\\)$";
    private static final String THIRD_LINE_PATTERN = VM_NAME + VENDOR_VERS + " \\(" + JDK_DEBUG + "build .*\\)$";
    private static final Pattern FIRST_PATTERN = Pattern.compile(FIRST_LINE_PATTERN);
    private static final Pattern SECOND_PATTERN = Pattern.compile(SECOND_LINE_PATTERN);
    private static final Pattern THIRD_PATTERN = Pattern.compile(THIRD_LINE_PATTERN);

    static int parse(InputStream is)
    {
        final BufferedReader stdInput = new BufferedReader(new InputStreamReader(is));
        try
        {
            final List<String> output = new ArrayList<>();
            String line;
            while ((line = stdInput.readLine()) != null)
            {
                output.add(line);
            }

            return parse(output.stream());
        }
        catch (IOException e)
        {
            return 0;
        }
    }

    private static int parse(Stream<String> output)
    {
        String stringOutput = output.collect(Collectors.joining("\n"));
        List<String> lines = stringOutput.lines()
            .dropWhile(l -> !l.startsWith("GraalVM") && !l.startsWith("native-image"))
            .toList();

        if (1 == lines.size())
        {
            throw new RuntimeException("Old, single line GraalVM version not supported");
        }

        final Matcher firstMatcher = FIRST_PATTERN.matcher(lines.get(0));
        final Matcher secondMatcher = SECOND_PATTERN.matcher(lines.get(1));
        final Matcher thirdMatcher = THIRD_PATTERN.matcher(lines.get(2));
        if (firstMatcher.find() && secondMatcher.find() && thirdMatcher.find())
        {
            final String javaVersion = secondMatcher.group(BUILD_INFO_GROUP);
            java.lang.Runtime.Version v;
            try
            {
                v = java.lang.Runtime.Version.parse(javaVersion);
                return v.feature();
            }
            catch (IllegalArgumentException e)
            {
                return 0;
            }
        }

        return 0;
    }
}
