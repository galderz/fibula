# fibula

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
Debugging benchmark generation for one of the tests:
```shell script
make test MAVEN_DEBUG=process DECOMPILE=true GEN=BenchmarkStateOrderTest
```

## TODO

- [ ] Print executing/results as TRACE and make results/params presented not in base64 format
- [ ] Separate building bootstrap+runner from running it (to avoid repeat build if source not changed)
- [ ] Avoid the BenchmarkList file and instead emit a build item and use the recorder
- [ ] Support running jvm mode with native image agent
      Doing this will require fixing the forking to avoid errors related to writing to same native image config file
      Once that is fixed, maybe default to running with only 1 fork and 1 warmup and 1 measurement fork, because that should be enough
- [ ] Add a record equals/hashCode benchmark
      Some potential examples:
      https://github.com/openjdk/jdk/blob/master/test/micro/org/openjdk/bench/java/io/RecordDeserialization.java
- [ ] See how record equals/hashCode benchmark behaves with different GraalVM versions
- [ ] Add profiler integration
      `perf annotate` is one option
      , another is to use `perf stat` along with `perf norm`
- [ ] Narrow down native issue with commons match dependency

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/fibula-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
