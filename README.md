# Fibula

Fibula allows you to run JMH benchmarks as GraalVM native executables.

# Pre-requisites

Build JMH
[`1.38-patches` branch](https://github.com/galderz/jmh/tree/1.38-patches)
locally to include the following patches:

* Install `jmh-core-it` tests jar locally.
This enables JMH integration tests to be run with Fibula.
* Fix for [Perf event validation not working with skid options](https://bugs.openjdk.org/browse/CODETOOLS-7903740) bug.
* Print exception in case the exception truncates the stream (e.g. serialization error).

```shell
git clone https://github.com/galderz/jmh && cd jmh
git checkout 1.38-patches
mvn install -DskipTests
```

> **IMPORTANT**:
> Do not use GraalVM to build JMH because annotation processor does not work as expected,
> and therefore no JMH benchmark source will be generated.
> Instead we recommend you use standard JDK releases,
> e.g. Adoptium Eclipse Temurin.

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
cd fibula-show/2408-jvmls/fibula
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

> **NOTE**:
> For those familiar with Quarkus,
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

> **TIP**: The generated bytecode can be debugged by building with `quarkus.package.jar.decompiler.enabled` option.
> The decompiled output can be located at the `target/decompiled` folder.

However, the package and module where `GraalDirectives` is located changed between GraalVM versions.
By default, Fibula configures the package and module locations as they were up to GraalVM for JDK 21.
If using GraalVM for JDK 22 or higher, pass in
`-Dfibula.graal.compiler.module=jdk.graal.compiler -Dfibula.graal.compiler.package.prefix=jdk.graal`
properties to adjust the module and package names.

## JVM Mode

Benchmarks can also run in JVM mode with Fibula.
There are several reasons why this might be useful:

1. Faster development cycle loop.
Native images are slow to build,
so it is more productive to develop Fibula running JMH benchmarks in JVM mode.
When things are working switch to native to make sure it works there too.
2. At times performance of JMH benchmarks in native might be different to JMH with HotSpot.
Running Fibula in JVM mode can help detect issues specific to the Fibula integration.

To do, just remove the `-Dnative` argument to build the benchmarks as a standard JVM application:

```shell
mvn package
```

The benchmark(s) run the same way,
no matter if built for native or JVM:

```shell
java -jar target/benchmarks.jar MyFirst
```

> **IMPORTANT**:
> There are no flags in Fibula to decide whether ro run a native or JVM runner application.
> Instead, the bootstrap makes the decision based on whether the native or JVM runner applications have been previously built.
> If it finds both the native and JVM runner applications,
> it will run the JVM application.

## Architecture

JMH benchmarks work by having a runner process coordinate benchmark runners embedded in the same process,
or forked runners running in separate processes.
When running inside a JVM, there's no real reason to separate the codebases for both sides into separate modules,
hence JMH's `jmh-core` module contains all the logic for both sides.

Fibula's aim is to run the benchmark runners as GraalVM native image executables,
and therefore it splits the code such that only the benchmark runner is converted into native executables.
The benefits of having this split are the following:

* By only converting the benchmark runner into native, 
a smaller universe has to be transformed which makes it native image process less error-prone.
A smaller universe also means smaller native executables and less moving parts.
* The benchmark runner can still run as a separate JVM process,
which means the native image agent can be plugged into it to detect any missing configuration.

The downsides of this split are:

* It is not possible to Fibula in native mode and `fork=0` mode,
where benchmark runner runs embedded in the same process as the coordinator/bootstrap process.
This is small price to pay since this mode is mostly used for testing and not to gather meaningful benchmark results.
* A more complex build and module structure is required in Fibula's source tree.

Following is a description of Fibula's modules and their jobs:

### `fibula-bootstrap`

`fibula-bootstrap` module is a JVM application that coordinates benchmarks.
It's main job is to run `org.openjdk.jmh.runner.Runner` process,
but it does not run this class directly.
Instead, it extends it to achieve two objects:

* It overrides the `getForkedMainCommand` method
in order to run benchmarks either as native executables or JVM applications.
JMH's implementation hardcodes the command to be a JVM application that runs `org.openjdk.jmh.runner.ForkedMain` class. 
Fibula runs either the `fibula-runner` application either as a native executable or a JVM application.
The method is package private,
so the override is achieved by using the same JMH package name.
* Amends `jvm`, `jvmArgs`, `jdkVersion`, `vmName`, `vmVersion` and `jmhVersion` presented in the console when the benchmark starts,
in order to present relevant information to a Fibula run.
For example, `vmName` for native executables will be `Substrate VM`,
and vm invoker shows the native binary rather than the `java` process.
To amend these fields Fibula wraps the `OutputFormat` and overrides `startBenchmark` method to apply the changes before they are shown to the user.
Also, since these fields are private and final, it uses reflection to modify them.

### `fibula-runner`

`fibula-runner` module is the Java application that actually runs the benchmark code.
Although it's actually a Quarkus application, no Quarkus application is run per-se.
It uses the method explained
[here](https://quarkus.io/blog/magic-control/#the-almost-no-magic-approach)
in order to avoid starting any Quarkus components, not even a CDI container.
This is particularly good for user benchmarks that use CDI,
because by not running the Quarkus CDI layer,
there is no risk of CDI compatibility issues.

> **NOTE**:
> `fibula-bootstrap` and `fibula-runner` communicate using the same binary client/server architecture that JMH uses.

### `fibula-it`

`fibula-it` is the integration testsuite containing homegrown tests,
and tests that run against JMH benchmarks defined in JMH's `jmh-core-it` module.
It reuses as much as JMH's code for testing,
which includes benchmark definitions,
but because JMH does not abstract away how the benchmark is constructed,
the benchmark construction and assertions are duplicated.

### `fibula-samples`

`fibula-samples` is a module containing JMH benchmark samples created to demonstrate some of the JMH features Fibula currently supports.
On top of that,
this module depends on JMH's `jmh-samples` module,
so it can be used to run any of JMH's own samples with Fibula.

### `fibula-extension`

`fibula-extension` is a Quarkus extension whose job is to enable JMH benchmarks to run as GraalVM native images.
Its main responsibility is to generate bytecode to run the JMH benchmarks for the user code provided,
and include that in the Quarkus application that gets converted into native image.
To achieve this, first it has to generate JMH source code from the annotated user code.
This is done using JMH's reflection bytecode generator,
whose API takes in as main parameter the directory where the benchmark code is compiled,
and transforms the compiled classes into benchmark execution java source code.

> **NOTE**:
> Traditionally JMH applications use annotation processing integration to achieve this.
Fibula could have used the same annotation processing,
but it would have required bridging over to Jandex,
which is where annotations metadata is stored in Quarkus applications.
Building the bridge require non-trivial code,
and so it was deemed easier in this case to use the reflection bytecode generator instead.

Once the source code has been generated,
it needs to be compiled into java bytecode in order to include within the Quarkus application
that wraps the benchmark runner code.
The compilation is done using the in-memory Java compiler,
and it's output is emitted as a collection of Quarkus `GeneratedClassBuildItem` classes.
By doing this, the generated benchmark bytecode becomes part of the generated bytecode,
which Quarkus can then transform into native executables using GraalVM native image.

The extension has a few additional notable responsibilities:

* Generates bytecode to substitute JMH `org.openjdk.jmh.infra.Blackhole` API calls
  with GraalVM compiler blackhole API methods,
  using its `GraalDirectives` class.
  This is an essential piece for blackhole invocations running as native executables to behave just as JMH expects it.
* Registers all generated classes with `jmhTest` suffix for reflection access.
  This is necessary so that the JMH's `org.openjdk.jmh.runner.ForkedMain` can execute them as needed.
* `org.openjdk.jmh.runner.InfraControl` is marked for runtime initialization
  because it contains a static block that uses unsafe to evade false sharing,
  and these field offsets need to be computed at runtime.
* `org.openjdk.jmh.runner.InfraControl` is also registered for reflection
  so that all padded fields that would appear unused to the GraalVM native image points-to analysis are not dead-code eliminated.
* The contents of all `BenchmarkList` metadata files found in user benchmark dependencies,
  as well as the metadata for the user benchmarks,
  are stored into a single `BenchmarkList` file.
  This enables benchmarks in the current project and in any of its dependencies to be located successfully.

## JMH Wishlist

aka "The shopping list for Shipilev".

The patches in the JMH [`1.38-patches` branch](https://github.com/galderz/jmh/tree/1.38-patches).

Make `org.openjdk.jmh.runner.ForkedMain` public.
This is the entry point for the runner process.
Fibula currently uses reflection to access and instantiate it.

Switch `org.openjdk.jmh.runner.Runner.getForkedMainCommand`
from package private to protected.
That way it can be extended without the need to create a split package situation.

A cleaner way to adjust the target runner specific benchmark parameters.
`jvm` and `jvmArgs` could be adjusted by wrapping `Options` around,
and returning the correct values based on the target runner.
`jdkVersion`, `vmName`, `vmVersion` and `jmhVersion` are trickier to easily adjust.
Although `PrintPropertiesMain` can help when the target jvm and the bootstrap jvm are different,
it falls short to cover scenarios like this.
An alternative way would be to extend `AbstractOutputFormat`,
wrap the original `OutputFormat` instance,
and reimplement the `startBenchmark` logic.
The issue with this approach is that `startBenchmark` is a lengthy method,
but the benefit is that no reflection is needed.

More of the `jmh-core-it` code could be reused by Fibula if the way the `Runner` instance is constructed would be abstracted away.
Doing so would enable not only benchmark annotated code to be reused,
but the code that starts the runner and asserts expectations as well.

### Exceptions in native benchmarks

Any of the annotated methods in JMH benchmarks can throw an exception,
which gets serialized and sent back from the forked (runner) process,
back to the bootstrap process.
Serializing these exceptions in native requires pre-registration of the exceptions,
so that GraalVM native image knows how to serialize them.

However, there's currently no easy way to know in advance which exceptions will be thrown at runtime,
and the spectrum of potential exceptions that could be thrown is infinite.
Fibula registers some commonly thrown exceptions (like `NullPointerException`),
but for the majority of exceptions users will get errors like this when running Fibula in native mode:

```shell
com.oracle.svm.core.jdk.UnsupportedFeatureError: SerializationConstructorAccessor class not found for declaringClass: org.mendrugo.fibula.samples.SampleCustomException (targetConstructorClass: java.lang.Object). Usually adding org.mendrugo.fibula.samples.SampleCustomException to serialization-config.json fixes the problem.
```

The best fix here is really to fix to the source of exception,
because for benchmarks results to be meaningful,
they should not throw any exceptions.

Eventually there might be a way for the user to define exceptions that the benchmark might throw in advance,
and for Fibula to read those and register them for serialization in GraalVM native image.

## Makefile Guide

The project contains a Makefile designed to speed up typical development tasks.
Here's a short guide on how to use it:

Run individual benchmarks in JVM mode:
```shell script
make run BENCHMARK=JMHSample_01
```
Run individual benchmarks in JVM mode and debug the boostrap process:
```shell script
make run BENCHMARK=JMHSample_01 DEBUG=true
```
Run individual benchmarks in JVM mode and debug the runner process:
```shell script
make run BENCHMARK=JMHSample_01 RUNNER_DEBUG=true
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
Run tests with native agent:
```shell script
make test NATIVE_AGENT=true
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
