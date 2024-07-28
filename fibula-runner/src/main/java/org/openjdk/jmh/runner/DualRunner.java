package org.openjdk.jmh.runner;

import org.mendrugo.fibula.results.IterationError;
import org.mendrugo.fibula.results.Serializables;
import org.mendrugo.fibula.runner.IterationClient;
import org.openjdk.jmh.results.BenchmarkResultMetaData;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.options.Options;

public class DualRunner extends BaseRunner
{
    private final IterationClient iterationClient;

    public DualRunner(Options options, OutputFormat handler, IterationClient iterationClient)
    {
        super(options, handler);
        this.iterationClient = iterationClient;
    }

    public void run(ActionPlan actionPlan)
    {
        try
        {
            final IterationResultAcceptor acceptor = new IterationResultAcceptor()
            {
                @Override
                public void accept(IterationResult iterationResult)
                {
                    iterationClient.notifyResult(Serializables.toBase64(iterationResult));
                }

                @Override
                public void acceptMeta(BenchmarkResultMetaData resultMetaData)
                {
                    iterationClient.notifyResultMetaData(Serializables.toBase64(resultMetaData));
                }
            };

            runBenchmarksForked(actionPlan, acceptor);
        }
        catch (BenchmarkException be)
        {
            iterationClient.notifyError(IterationError.of(be));
        }
    }
}
