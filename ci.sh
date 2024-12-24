#!/usr/bin/env bash

set -e -x

sample()
{
    local run_target=$1

    BENCHMARK=JMHSample_09 TU=ns make $run_target
    BENCHMARK=FibulaSample_01_MultiHelloWorld make $run_target
}

make clean
# native tests already run jvm mode tests before
make test
sample "run-jvm"
sample "run"
