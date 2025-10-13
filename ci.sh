#!/usr/bin/env bash

set -e -x

sample()
{
    local run_target=$1

    BENCHMARK=JMHSample_09 TU=ns gmake $run_target
    BENCHMARK=FibulaSample_01_MultiHelloWorld gmake $run_target
}

pgo()
{
    gmake run
    gmake run-pgo
    awk -F, '
    NR==FNR && NR==2 {a=$5; print "a from aot-result.csv: " a; next}
    FNR==2 {b=$5; print "b from pgo-result.csv: " b}
    END {print "Final a: " a ", Final b: " b; if (b >= a * 1.1) print "Passed: Score in pgo-result.csv is at least 10% bigger"; else print "Failed: Score in pgo-result.csv is less than 10% bigger"}
    ' fibula-samples/target/aot-result.csv fibula-samples/target/pgo-result.csv
}

gmake clean
# native tests already run jvm mode tests before
gmake test
sample "run-jvm"
sample "run"
pgo
