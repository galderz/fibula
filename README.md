# Fibula

Fibula allows you to run JMH benchmarks as GraalVM native executables.

# Pre-requisites

There are no Fibula releases yet so you need to build Fibula first.

Building Fibula requires JDK 21.
Set `JAVA_HOME` to GraalVM for JDK 21 and execute:

```
./mvnw install
```

> **NOTE**: Mandrel does not currently work with benchmarks relying on blackholes.

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
but equivalent functionality can be obtained with the `DwarfPerfAsmProfiler`.
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

Finally, you can run the benchmark passing in `-prof org.mendrugo.fibula.DwarfPerfAsmProfiler`
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
> For example: `-prof org.mendrugo.fibula.DwarfPerfAsmProfiler:events=cycles:P`.
> This option is only available when Fibula is built with a JMH snapshot version from the master branch.

## Blackholes

Both implicit and explicit blackholes are supported,
but additional configuration might be required when building the benchmarks depending on the GraalVM version.

Fibula generates bytecode to invoke one of the available `GraalDirectives.blackhole` methods,
which works with the GraalVM compiler to make sure values sent to the blackhole are not optimized away.

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
make run BENCHMARK=JMHSample_01
```
Run individual benchmarks in JVM mode:
```shell script
make run-jvm BENCHMARK=JMHSample_01
```
Run individual benchmarks in JVM mode and remote debug the runner JVM process on port 8000:
```shell script
make run BENCHMARK=JMHSample_01 DEBUG=true
```
Run tests in JVM mode with native agent:
```shell script
make test-jvm NATIVE_AGENT=true
```
Run individual test in native mode:
```shell
make test TEST=BlackholesTest
```
Run individual JVM mode test:
```shell
make test-jvm TEST=BlackholesTest
```
Debugging benchmark generation for one of the tests with the IDE:
```shell script
make test MAVEN_DEBUG=process TEST=BlackholesTest
```
Debugging benchmark test execution for one of the tests:
```shell script
make test MAVEN_DEBUG=test TEST=FailureModesTest
```
Running individual test method in native:
```shell script
make test TEST="FailureModesTest#shouldFailOnCustomExceptionAtBenchmark"
```
Running a benchmark with Linux perf or perfnorm stat profiler:
```shell script
make run BENCHMARK=JMHSample_01 PROF=perf
make run BENCHMARK=JMHSample_01 PROF=perfnormc
```
Running a benchmark sample in Native mode with perf stat or perf norm profiler with only branches and instructions
(needs JMH master build to avoid multiplexing):
```shell script
make run BENCHMARK=JMHSample_01 PROF=perf:events=branches,instructions,cycles
make run BENCHMARK=JMHSample_01 PROF=perfnorm:events=branches,instructions,cycles
```
Running a benchmark sample in JVM mode with `perf` or `perfnorm` profiler:
```shell script
make run-jvm BENCHMARK=JMHSample_01 PROF=perf
make run-jvm BENCHMARK=JMHSample_01 PROF=perfnorm
```
Running a benchmark sample in Native mode with perfasm that uses DWARF call graph and saving the perf bin data:
```shell script
make run BENCHMARK=JMHSample_01 PROF=org.mendrugo.fibula.DwarfPerfAsmProfiler:events=cycles:P DEBUG_INFO=true MEASURE_TIME=1 WARMUP_TIME=1
```
