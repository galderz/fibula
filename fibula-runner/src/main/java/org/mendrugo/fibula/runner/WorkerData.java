package org.mendrugo.fibula.runner;

import org.openjdk.jmh.infra.ThreadParams;

public final class WorkerData
{
    public final Object instance;
    public final ThreadParams threadParams;

    public WorkerData(Object instance, ThreadParams threadParams)
    {
        this.instance = instance;
        this.threadParams = threadParams;
    }
}
