package org.mendrugo.fibula;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.mendrugo.fibula.results.ThroughputResult;

@Path("/api/results")
public class ResultResource
{
    @POST
    public String add(ThroughputResult result)
    {
        final int COLUMN_PAD = 2;

        // todo unfix
        final String benchmarkName = "FibulaSample_01_HelloWorld.helloWorld";
        final String modeName = "thrpt";

        int nameLength = "Benchmark".length();
        nameLength = Math.max(nameLength, benchmarkName.length());

        int modeLength = "Mode".length();
        modeLength = Math.max(modeLength, modeName.length());

        int samplesLength = "Cnt".length();

        int scoreLength = "Score".length();
        scoreLength = Math.max(scoreLength, ScoreFormatter.format(result.statistic()).length());

        int scoreErrLength = "Error".length();

        int unitLength = "Units".length();

        modeLength += COLUMN_PAD;
        samplesLength += COLUMN_PAD;
        scoreLength += COLUMN_PAD;
        scoreErrLength += COLUMN_PAD - 1; // digest a single character for +- separator
        unitLength += COLUMN_PAD;

        System.out.printf("%-" + nameLength + "s", "Benchmark");
        System.out.printf("%" + modeLength + "s", "Mode");
        System.out.printf("%" + samplesLength + "s", "Cnt");
        System.out.printf("%" + scoreLength + "s", "Score");
        System.out.print("  ");
        System.out.printf("%" + scoreErrLength + "s", "Error");
        System.out.printf("%" + unitLength + "s",     "Units");
        System.out.println();

        System.out.printf("%-" + nameLength + "s", benchmarkName);
        System.out.printf("%" + modeLength + "s", modeName);
        System.out.print(ScoreFormatter.format(scoreLength, result.statistic()));
        System.out.print("  ");
        System.out.printf("%" + scoreErrLength + "s", "");
        System.out.printf("%" + unitLength + "s", result.unit());
        System.out.println();

        return "OK";
    }
}
