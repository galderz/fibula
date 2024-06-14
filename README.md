# Fibula

Fibula allows you to run JMH benchmarks as GraalVM native executables.

# Pre-requisites

Build JMH [`1.37-patches` branch](https://github.com/galderz/jmh/tree/1.37-patches) locally to include the following patches:

* Install `jmh-core-it` tests jar locally.
This enables JMH integration tests to be run with Fibula.
* Fix for [`perf stat` command not constructed right for event selection](https://bugs.openjdk.org/browse/CODETOOLS-7903739) bug.
* Fix for [Perf event validation not working with skid options](https://bugs.openjdk.org/browse/CODETOOLS-7903740) bug.

```shell
git clone https://github.com/galderz/jmh && cd jmh
git checkout 1.37-patches
mvn install -DskipTests
```

Build Fibula with JDK 21 or newer:

```shell
mvn install -DskipTests
```

Install GraalVM or Mandrel for JDK 21.

## Getting Started

To run the first JMH benchmark using Fibula,
checkout the
[fibula-show](https://github.com/galderz/fibula-show) repository,
and navigate to the Fibula sample project:

```shell
git clone https://github.com/galderz/fibula-show
cd fibula-show/2406-team/fibula
```

Set the `JAVA_HOME` to GraalVM or Mandrel for JDK 21,
and build the benchmark:

```shell
mvn package -Dnative
```

Run the benchmark:
```shell
java -jar target/benchmarks.jar MyFirst
```

## Decompiling

Fibula generates bytecode to wrap benchmarks around infrastructure required to measure performance.
This bytecode can optionally be decompiled for closer inspection.
To do that, build the Fibula benchmark with `quarkus.package.jar.decompiler.enabled`.
For example:

```shell
mvn package -Dnative -Dquarkus.package.jar.decompiler.enabled
```

The decompiled output can be located at the `target/decompiled` folder.

## Logging

It is possible to enable logging by changing the log level globally,
e.g. `-Dquarkus.log.level=DEBUG`,
or per category,
e.g. `-Dquarkus.log.category.\"org.mendrugo.fibula\".level=DEBUG`.

## Profiling

`perf` and `perfnorm` can be used just like with JMH.

JMH `perfasm` profiler is not yet fully supported,
but equivalent functionality can be obtained with the `DwarfPerfAsmProfiler`.
This custom profiler extends the `perf record` arguments to configure `dwarf` callgraph. 

To use this profiler,
the benchmark needs to be built with DWARF debug info,
and instruct native image to keep local symbols:

```shell
mvn package -Dnative -Dquarkus.native.debug.enabled -Dfibula.native.additional-build-args=-H:-DeleteLocalSymbols
```

> **_NOTE:_** For those familiar with Quarkus,
> note that deleting local symbols is passed via `fibula.native.additional-build-args` instead of `quarkus.native.additional-build-args`.
> This is needed because Fibula already defines configuration for `quarkus.native.additional-build-args` internally,
> and so the contents of `fibula.native.additional-build-args` get appended to it.

Finally, you can run the benchmark passing in `-prof org.mendrugo.fibula.bootstrap.DwarfPerfAsmProfiler`
and when the benchmark finishes a `.perfbin` file will be generated in the working folder.
Use `perf annotate -i < <file>.perfbin` to analyse hot assembly parts, e.g.

```bash
  8.87 │90:┌──cmpb       $0x0,0xc(%rsi)
  8.49 │   ├──jne        b0
  8.61 │   │  mov        %rax,%rcx
 16.68 │   │  inc        %rcx
  7.46 │   │  subl       $0x1,0x10(%r15)
 16.45 │   │↓ jle        d9
 17.27 │   │  mov        %rcx,%rax
 16.16 │   │↑ jmp        90
       │b0:└─→mov        %rax,0x20(%rsp)
```

> **_TIP:_** Pass in `:P` event modifier to avoid performance events skid.
> For example: `-prof org.mendrugo.fibula.bootstrap.DwarfPerfAsmProfiler:events=cycles:P`

## Blackholes

Both implicit and explicit blackholes are supported,
but additional configuration might be required when building the benchmarks depending on the GraalVM version.

Fibula generates bytecode to invoke one of the available `GraalDirectives.blackhole` methods,
which works with the GraalVM compiler to make sure values sent to the blackhole are not optimized away.

However, the package and module where `GraalDirectives` is located changed between GraalVM versions.
By default, Fibula configures the package and module locations as they were up to GraalVM for JDK 21.
If using GraalVM for JDK 22 or higher, pass in
`-Dfibula.graal.compiler.module=jdk.graal.compiler -Dfibula.graal.compiler.package.prefix=jdk.graal`
properties to adjust the module and package names.

## JMH Features Checklist

These are the JMH features that Fibula currently supports:

- [x] Throughput and average benchmark modes.
- [x] Implicit blackhole support for returned values.
- [x] Explicit `Blackhole` benchmark parameters.
- [x] Output time unit definitions via `@OutputTimeUnit` annotation.
- [x] `@Setup` and `@TearDown` annotations.
- [x] `perf` and `perfnorm` profilers.

Some JMH features are partially supported:

* `perfasm` profile (see "Profiling" section for details).
* `State` annotated objects with `Benchmark` and `Thread` scopes supported,
but annotation not yet supported on super classes.

It is possible to run Fibula against existing
[JMH Samples](https://github.com/Frankqsy/jmh-samples/tree/master/src/main/java/org/openjdk/jmh/samples),
but since not all JMH features are supported yet,
Fibula explicitly excludes unsupported samples.
Therefore, inspect the source code before trying to run a specific JMH sample to see if it's supported.

## JMH Wishlist

aka "The shopping list for Shipilev".

Switch the following types from package private to public:
- [ ] `org.openjdk.jmh.generators.core.BenchmarkInfo`
- [ ] `org.openjdk.jmh.generators.core.CompilerControlPlugin`
- [ ] `org.openjdk.jmh.generators.core.HelperType`
- [ ] `org.openjdk.jmh.generators.core.HelperMethodInvocation`
- [ ] `org.openjdk.jmh.generators.core.MethodGroup`
- [ ] `org.openjdk.jmh.generators.core.StateObject`
- [ ] `org.openjdk.jmh.generators.core.StateObjectHandler`

Switch the following methods from private to public:
- [ ] `org.openjdk.jmh.generators.core.StateObjectHandler.stateOrder()`
- [ ] `org.openjdk.jmh.generators.core.BenchmarkGenerator.buildAnnotatedSet()`
- [ ] `org.openjdk.jmh.generators.core.BenchmarkGenerator.makeBenchmarkInfo()`

Switch the following fields to public or make public accessors
- [ ] `org.openjdk.jmh.generators.core.StateObjectHandler.stateObjects`

Fibula works around the limitations above using either of these techniques:

* To avoid having to use reflection for accessing package private classes,
Fibula classes that need access to these APIs are defined inside `org.openjdk.jmh.generators.core` package.
For example: `JmhBenchmarkGenerator` or `JmhStateObjectHandler`.
* Use reflection to unlock private method/field access.

## Architecture

TODO: review and complete

`fibula-bootstrap` module is a Quarkus JVM application that coordinates benchmarks.
It starts an HTTP REST endpoint and waits for data from forked runners.

`fibula-runner` module is the Quarkus application that actually runs the benchmark code.
It is a command line application that uses a HTTP REST client to communicate with the bootstrap process.

`fibula-benchmarks` module enables end-user experience akin to JMH,
whereby users expect a single `benchmarks.jar` to execute.
It achieves that by depending solely on the `fibula-bootstrap` module,
which is the module where benchmark coordination begins,
and then making the module an `uber-jar`.
This effectively transforms the `fibula-bootstrap` module,
and all of its dependencies,
into a `benchmarks.jar` uber-jar.

## Makefile Guide

Run individual benchmarks in JVM mode:
```shell script
make run BENCHMARK=JMHSample_01
```
Run individual benchmarks in Native mode:
```shell script
make run-native BENCHMARK=JMHSample_01
```
Running a benchmark with Linux perf stat profiler:
```shell script
make run-native BENCHMARK=JMHSample_01 PROFILER=perf
```
Decompile generated bytecode:
```shell script
make test DECOMPILE=true
```
Debugging benchmark generation for one of the tests with the IDE:
```shell script
make test MAVEN_DEBUG=process GEN=BenchmarkStateOrderTest
```
Debugging benchmark test execution for one of the tests:
```shell script
make test MAVEN_DEBUG=test TEST=FailureModesTest
```
Running a benchmark sample in Native mode with perf or perfnorm profiler:
```shell script
make run-native BENCHMARK=JMHSample_01 PROF=perf
make run-native BENCHMARK=JMHSample_01 PROF=perfnorm
```
Running a benchmark sample in Native mode with perf stat or perf norm profiler with only branches and instructions
(needs https://github.com/galderz/jmh/commit/4000d778664e5a138fef8b8b79d3a823fa843527 to avoid multiplexing):
```shell script
make run-native BENCHMARK=JMHSample_01 PROF=perf:events=branches,instructions,cycles
make run-native BENCHMARK=JMHSample_01 PROF=perfnorm:events=branches,instructions,cycles
```
Running a benchmark sample in JVM mode with `perf` or `perfnorm` profiler:
```shell script
make run BENCHMARK=JMHSample_01 PROF=perf
make run BENCHMARK=JMHSample_01 PROF=perfnorm
```
Running a benchmark sample in Native mode with perfasm that uses DWARF call graph and saving the perf bin data
(needs https://github.com/galderz/jmh/commit/e623b6a93263fa24d5ec11076b59b3ea3b8dd0fc to enable modifiers):
```shell script
make run-native BENCHMARK=JMHSample_01 PROF=org.mendrugo.fibula.bootstrap.DwarfPerfAsmProfiler:events=cycles:P DEBUG_INFO=true MEASURE_TIME=1 WARMUP_TIME=1
```
