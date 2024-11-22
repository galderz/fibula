#!/usr/bin/env bash

set -e -x

test()
{
    local run_target=$1

    BENCHMARK=JMHSample_01 make $run_target
    BENCHMARK=FibulaSample_01_MultiHelloWorld make $run_target
}

make clean
test "run"
test "run-native"
