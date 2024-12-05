#!/usr/bin/env bash

set -e -x

sample()
{
    local run_target=$1

    BENCHMARK=JMHSample_09 make $run_target
    BENCHMARK=FibulaSample_01_MultiHelloWorld make $run_target
}

make clean
# native tests already run jvm mode tests before
make test-native
sample "run"
sample "run-native"
