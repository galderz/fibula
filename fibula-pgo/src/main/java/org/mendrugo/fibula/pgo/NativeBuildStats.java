package org.mendrugo.fibula.pgo;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

class NativeBuildStats
{
    private final String[] pgo;

    private NativeBuildStats(String[] pgo)
    {
        this.pgo = pgo;
    }

    boolean hasPgoInstrument()
    {
        return Arrays.asList(pgo).contains("instrument");
    }

    static NativeBuildStats fromJson(String json)
    {
        final Moshi moshi = new Moshi.Builder().build();
        final JsonAdapter<BuildStats> jsonAdapter = moshi.adapter(BuildStats.class);
        try
        {
            final BuildStats buildStats = jsonAdapter.fromJson(json);
            if (buildStats == null
                || buildStats.general_info == null
                || buildStats.general_info.graal_compiler == null
                || buildStats.general_info.graal_compiler.pgo == null)
            {
                return new NativeBuildStats(new String[0]);
            }

            return new NativeBuildStats(buildStats.general_info.graal_compiler.pgo);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public record BuildStats(GeneralInfo general_info) {}

    public record GeneralInfo(GraalCompiler graal_compiler) {}

    public record GraalCompiler(String[] pgo) {}
}
