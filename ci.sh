#!/usr/bin/env bash

set -e -x

test()
{
    local test_target=$1
    local run_target=$2

    make clean
    make $test_target
    BENCHMARK=JMHSample_01 make $run_target
    BENCHMARK=FibulaSample_01_MultiHelloWorld make $run_target
}

test "test" "run"
test "test-native" "run-native"
