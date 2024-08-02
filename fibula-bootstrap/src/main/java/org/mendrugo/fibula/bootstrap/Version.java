package org.mendrugo.fibula.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

final class Version
{
    String getVersion()
    {
        final Properties p = new Properties();
        try (InputStream s = Version.class.getResourceAsStream("/fibula.properties"))
        {
            p.load(s);
            String version = (String) p.get("fibula.version");
            return Objects.requireNonNullElse(version, "-");
        }
        catch (IOException e)
        {
            return "-";
        }
    }
}
