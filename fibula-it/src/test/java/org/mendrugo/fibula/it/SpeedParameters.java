package org.mendrugo.fibula.it;

record SpeedParameters(
    int timeoutMins
    , String measureTime
    , int measureForkCount
    , int measureIterationCount
    , String warmupTime
    , int warmupIterationCount
) {}
