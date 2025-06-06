# Fibula

Fibula allows you to run JMH benchmarks as GraalVM native executables.

# Pre-requisites

There are no Fibula releases yet so you need to build Fibula first.
But before building Fibula, JMH itself needs to built since Fibula requires small modifications to avoid code duplication.
JMH can be built with any recent JDK LTS versions, e.g. JDK 21:

```shell
git clone https://github.com/galderz/jmh
cd jmh && git checkout topic.fibula-refactors
mvn install -DskipTests
```

Building Fibula requires JDK 21.
Set `JAVA_HOME` to GraalVM for JDK 21 and execute:

```shell
./mvnw install
```

## Getting Started

To run the first JMH benchmark using Fibula,
checkout the
[fibula-show](https://github.com/galderz/fibula-show) repository,
and navigate to the Fibula sample project:

```shell
git clone https://github.com/galderz/fibula-show
cd fibula-show/2412-strings
```

Set the `JAVA_HOME` to GraalVM for JDK 21,
and build the benchmark:

```shell
mvn package
```

Run the benchmark:
```shell
java -jar target/benchmarks.jar
```

## Profiling

`perf` and `perfnorm` can be used just like with JMH.

JMH `perfasm` profiler is not yet fully supported,
but equivalent functionality can be obtained with the `PerfDwarfProfiler`.
This custom profiler extends the `perf record` arguments to configure `dwarf` callgraph. 

To use this profiler,
the benchmark needs to be built with DWARF debug info,
and instruct native image to keep local symbols:

```shell
mvn package -Ddebug=true -DbuildArgs=-H:-DeleteLocalSymbols
```

You can obtain more detailed profiling by using the following command instead.
The downside is that the performance degrades:

```shell
mvn package -Ddebug=true -DbuildArgs=-H:-DeleteLocalSymbols,-H:+SourceLevelDebug,-H:+TrackNodeSourcePosition,-H:+DebugCodeInfoUseSourceMappings
```

Finally, you can run the benchmark passing in `-prof org.mendrugo.fibula.PerfDwarfProfiler`
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
> For example: `-prof org.mendrugo.fibula.PerfDwarfProfiler:events=cycles:P`.
> This option is only available when Fibula is built with a JMH snapshot version from the master branch.

## Blackholes

Both implicit and explicit blackholes are supported.
Fibula generates bytecode to invoke one of the available `GraalDirectives.blackhole` methods,
which works with the GraalVM compiler to make sure values sent to the blackhole are not optimized away.

## Benchmarking Profile-Guided Optimizations (PGO)

It is possible to benchmark native images when they are compiled with PGO.
The integration here makes it seamless:

First it builds the benchmark with PGO instrumentation.
At runtime,
each benchmark runs a warmup fork with the PGO instrumented binary.
When the warmup fork finishes,
it rebuilds the native image using the profiling data obtained during the warmup fork,
and the benchmark continues as normal.
The final results presented are derived from the benchmark run with the optimized binary.

You can see this integration in action with
[this example](https://github.com/galderz/fibula-show/tree/main/2412-strings).

To run benchmarks with PGO,
Oracle GraalVM is a requirement.
The example Maven project contains a `pgo` profile that you can activate so that the instrumented binary is constructed:

```shell
mvn package -Dpgo
```

Run the benchmark as usual and obtain the results:

```shell
java -jar target/benchmarks.jar
```

> **WARNING**: The results obtained with PGO on microbenchmarks need to be handled with care.
> Although PGO can potentially present better results in a microbenchmark,
> in real life code execution might contain more variations.
> These variations can result in worse than expected performance,
> because the virtual machine cannot backoff the optimizations made with the profiling data captured during instrumentation.
> In other words, in native mode there is no re-optimize at method at runtime.
> If PGO aggressively optimizes a code path based on a profile, then at run-time it will only be able to fall back to a generic compilation,
> which might be worse than HotSpot can achieve since it can recompile code at runtime when access patterns change.

You should profile PGO optimized binaries to understand the nature of the optimizations.
To help with profiling,
the example Maven project contain a dedicated `pgo.perf` profile that adds the necessary build options:

```shell
mvn package -Dpgo.perf
```

Then run the benchmark using the same profiling options shown above, e.g.

```shell
java -jar target/benchmarks.jar -prof org.mendrugo.fibula.PerfDwarfProfiler:events=cycles:P
```

When the benchmark finishes,
you can use `perf annotate` to inspect the generated `.perfbin` files.
Remember that these files belong to the PGO optimized benchmark runs.

## JVM Mode

Benchmarks can also run in JVM mode with Fibula.
There are several reasons why this might be useful:

1. Faster development cycle loop.
Native images are slow to build,
so it is more productive to develop Fibula running JMH benchmarks in JVM mode.
When things are working switch to native to make sure it works there too.
2. At times performance of JMH benchmarks in native might be different to JMH with HotSpot.
Running Fibula in JVM mode can help detect issues specific to the Fibula integration.

To do, just add the `-Djvm.mode` argument to build the benchmarks as a standard JVM application:

```shell
mvn package -Djvm.mode
```

The benchmark(s) run the same way,
no matter if built for native or JVM:

```shell
java -jar target/benchmarks.jar
```

> **IMPORTANT**:
> There are no flags in Fibula to decide whether ro run a native or JVM runner application.
> Instead, the code decides based on whether the native or JVM runner applications have been previously built.
> If it finds both the native and JVM runner applications,
> it will run whichever is newer.

## Makefile Guide

The project contains a Makefile designed to speed up typical development tasks.
Here's a short guide on how to use it:

Run individual benchmarks in native mode:
```shell script
BENCHMARK=JMHSample_01 make run
```
Run individual benchmarks in JVM mode:
```shell script
BENCHMARK=JMHSample_01 make run-jvm
```
Run individual benchmarks in native mode and remote debug the runner JVM process on port 5005:
```shell script
DEBUG=runner BENCHMARK=JMHSample_01 make run
```
Run individual benchmarks in JVM mode and remote debug the runner JVM process on port 5005:
```shell script
DEBUG=runner BENCHMARK=JMHSample_01 make run-jvm
```
Run individual benchmarks in JVM mode and remote debug the forked JVM process on port 6006:
```shell script
DEBUG=fork BENCHMARK=JMHSample_01 make run-jvm
```
Run tests in JVM mode with native agent:
```shell script
NATIVE_AGENT=true make test-jvm
```
Run individual test in native mode:
```shell
TEST=BlackholesTest make test
```
Run individual JVM mode test:
```shell
TEST=BlackholesTest make test-jvm
```
Running individual test method in native:
```shell script
TEST="FailureModesTest#shouldFailOnCustomExceptionAtBenchmark" make test
```
Debugging annotation processor code one of the tests with the IDE on port 8000:
```shell script
DEBUG=maven TEST=BlackholesTest make test
```
Debugging the runner for a test on native mode on port 5005:
```shell script
DEBUG=runner TEST=FailureModesTest make test
```
Debugging the runner for a test on jvm mode on port 5005:
```shell script
DEBUG=runner TEST=FailureModesTest make test-jvm
```
Debugging the forked benchmark for a test on jvm mode on port 6006:
```shell script
DEBUG=fork TEST=FailureModesTest make test-jvm
```
Running a benchmark with Linux perf or perfnorm stat profiler:
```shell script
PROF=perf BENCHMARK=JMHSample_01 make run
PROF=perfnorm BENCHMARK=JMHSample_01 make run
```
Running a benchmark sample in Native mode with perf stat or perf norm profiler with only branches and instructions
(needs JMH master build to avoid multiplexing):
```shell script
PROF=perf:events=branches,instructions,cycles BENCHMARK=JMHSample_01 make run
PROF=perfnorm:events=branches,instructions,cycles BENCHMARK=JMHSample_01 make run
```
Running a benchmark sample in JVM mode with `perf` or `perfnorm` profiler:
```shell script
PROF=perf BENCHMARK=JMHSample_01 make run-jvm
PROF=perfnorm BENCHMARK=JMHSample_01 make run-jvm
```
Running a benchmark sample in Native mode with perfasm that uses DWARF call graph and saving the perf bin data:
```shell script
PROF=org.mendrugo.fibula.PerfDwarfProfiler:events=cycles:P BENCHMARK=JMHSample_01 DEBUG_INFO=true MEASURE_TIME=1 WARMUP_TIME=1 make run
```
