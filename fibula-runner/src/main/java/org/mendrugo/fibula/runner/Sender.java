package org.mendrugo.fibula.runner;

import org.mendrugo.fibula.results.ThroughputResult;
import org.mendrugo.fibula.runner.client.ResultProxy;

public class Sender
{
    static void send(ThroughputResult result, ResultProxy proxy)
    {
        proxy.send(result);
    }
}
