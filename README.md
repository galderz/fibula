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
mvn package -Dnative -Dquarkus.native.debug.enabled \
    -Dfibula.native.additional-build-args=-H:-DeleteLocalSymbols
```

> **NOTE**: For those familiar with Quarkus,
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

> **TIP**: Pass in `:P` event modifier to avoid performance events skid.
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

## JVM Mode

TODO explain jvm mode...

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

The patches in the JMH [`1.37-patches` branch](https://github.com/galderz/jmh/tree/1.37-patches).

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

`fibula-bootstrap` module is a Quarkus JVM application that coordinates benchmarks.
It starts an HTTP REST endpoint to receive data from forked runners that it launches.

`fibula-benchmarks` module enables end-user experience akin to JMH,
whereby users expect a single `benchmarks.jar` to execute.
It achieves that by depending solely on the `fibula-bootstrap` module,
which is the module where benchmark coordination begins,
and then making the module an `uber-jar`.
This effectively transforms the `fibula-bootstrap` module,
and all of its dependencies,
into a `benchmarks.jar` uber-jar.

`fibula-runner` module is the Quarkus application that actually runs the benchmark code.
It is a command line application that uses an HTTP REST client to communicate with the bootstrap process.

`fibula-results` is a module that contains classes that are common to the bootstrap and runner modules.
The module name does not clearly represent what it contains and the name is legacy from early prototypes.
A lot of classes in there should also go away,
but they remain until more functionality has been implemented,
and a clearer idea on what is really common.
There is no desire to simply refactor this module to `fibula-common`,
because such modules can often become kitchen sinks.
Once a clearer idea emerges on what is common,
the module will be renamed accordingly.

The bootstrap module interacts with the runner module via its command line API.
The runner module interacts with the boostrap module via the HTTP REST endpoint.
JMH serializable types are used in these interactions as much as possible,
e.g. `BenchmarkParams`, `ThroughputResult`...etc.
Their binary payloads are exchanged as base 64 encoded text.

`fibula-it` is the integration testsuite containing tests homegrown tests,
and tests that run against JMH benchmarks defined in JMH's `jmh-core-it` module.

`fibula-samples` is a module containing JMH benchmark samples created to demonstrate some of the JMH features Fibula currently supports.
On top of that,
this module depends on JMH's `jmh-samples` module,
so it can be used to run any of JMH's own samples with Fibula.
The module contains an integration testsuite that tried to verify JMH and Fibula performance was within certain %,
but testing has showed that this is not easy to quantify,
and it would be expensive to try to do it in a clean environment.
This module will be refactored very soon to leave it only with the ability to run benchmarks in JMH's module `jmh-samples`.
The integration testsuite part has been superseded by the `fibula-it` module.

### Entrypoint Duality

Building and running a JMH benchmark with Fibula is powered by two Quarkus applications,
which means there are 2 different application entry points:
the entry point for the JVM-mode `fibula-bootstrap` (aka bootstrap) process,
and the entry point for the `fibula-runner` (aka runner), which can be run in either native or JVM mode.
Benchmarks written by end users of Fibula users need to rely on both Quarkus applications simultaneously:

* On one hand, they need to depend on the runner process because bytecode needs to generated based on the JMH benchmarks(s) in the end-user project.
This bytecode then needs to be fed to the Quarkus build that creates the package that contains the runner plus the bytecode,
and builds it as either a native or JVM application.
* On the other hand, they need the bootstrap process (indirectly via the `fibula-benchmarks` process),
to be able to kickstart the benchmark coordination process,
and this will fire of either the native or JVM pre-built runner application.

To signal the 2 application entrypoints,
and allow selection of entrypoint depending on the use case,
Fibula relies on Quarkus' `@QuarkusMain` annotation and customizes its name based on the use case
(`bootstrap` or `runner`).
Selecting the use case to run happens this way:

* End user applications have a compile time dependency on `fibula-runner`,
which defines the main class as the `runner`. 
* End use applications enable the `maven-dependency-plugin` to depend on the bootstrap, indirectly via the `fibula-benchmarks` process.
This causes the `fibula-benchmarks` uber jar to be copied to the `target` folder.
The bootstrap module defines the main class as `bootstrap`.
Although the bootstrap process is executed as is, without any additional bytecode in it,
it reads the metadata generated when the Quarkus build run on the end-user JMH benchmark project.
  * There are no flags in Fibula to decide whether ro run a native or JVM runner application.
    Instead, the bootstrap makes the decision based on whether the native or JVM runner applications have been previously built.
    If it finds both the native and JVM runner applications,
    it will run the JVM application.

## Makefile Guide

The project contains a Makefile designed to speed up typical development tasks.
Here's a short guide on how to use it:

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
