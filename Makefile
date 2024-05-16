SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

ifeq ($(origin .RECIPEPREFIX), undefined)
  $(error This Make does not support .RECIPEPREFIX. Please use GNU Make 4.0 or later)
endif
.RECIPEPREFIX = >

BENCHMARK ?= JMHSample_01
GRAALVM_HOME ?= $(HOME)/opt/graal-21
JAVA_HOME ?= $(GRAALVM_HOME)

benchmarks_jar = target/benchmarks.jar
bootstrap_jar = fibula-bootstrap/target/quarkus-app/quarkus-run.jar
java = $(JAVA_HOME)/bin/java
samples_runner_jvm = fibula-samples/target/quarkus-app/quarkus-run.jar
samples_runner_native = fibula-samples/target/fibula-samples-1.0.0-SNAPSHOT-runner
test_runner_jvm = fibula-it/target/quarkus-app/quarkus-run.jar
test_runner_native = fibula-it/target/fibula-it-1.0.0-SNAPSHOT-runner

system_props =
ifdef LOG_LEVEL
  ifeq ($(LOG_LEVEL),DEBUG)
    system_props += -Dquarkus.log.category.\"org.mendrugo.fibula\".level=DEBUG
  endif
endif

MEASURE_FORKS ?= 2
MEASURE_IT ?= 2
MEASURE_TIME ?= 2
WARMUP_IT ?= 2
WARMUP_TIME ?= 2
PROFILER ?=

ifdef PROFILER
  MEASURE_FORKS = 1
  MEASURE_IT = 1
  MEASURE_TIME = 10
  WARMUP_IT = 1
  WARMUP_TIME = 10
endif

# Benchmark name
benchmark_params += $(BENCHMARK)
benchmark_params += -f
benchmark_params += $(MEASURE_FORKS)
benchmark_params += -i
benchmark_params += $(MEASURE_IT)
benchmark_params += -r
benchmark_params += $(MEASURE_TIME)
benchmark_params += -wi
benchmark_params += $(WARMUP_IT)
benchmark_params += -w
benchmark_params += $(WARMUP_TIME)

ifdef RESULT_FORMAT
  benchmark_params += -rf
  benchmark_params += $(RESULT_FORMAT)
endif

ifdef PROFILER
  benchmark_params += -prof
  benchmark_params += $(PROFILER)
endif

MAVEN_DEBUG ?=

mvnw += JAVA_HOME=$(JAVA_HOME)
mvnw_runner += JAVA_HOME=$(JAVA_HOME)
ifeq ($(MAVEN_DEBUG),process)
  mvnw_runner += $(HOME)/opt/maven/bin/mvnDebug
else
  mvnw_runner += $(HOME)/opt/maven/bin/mvn
endif

mvnw += $(HOME)/opt/maven/bin/mvn

ifdef MAVEN_VERBOSE
  mvnw_runner += -X
endif

ifdef DEBUG
  java += -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000
endif

test_args = test
ifdef TEST
  test_args += -Dtest=$(TEST)
endif

runner_build_args =
ifdef DECOMPILE
  runner_build_args += -Dquarkus.package.vineflower.enabled=true
endif
ifdef GRAALVM_VERSION
  ifeq ($(GRAALVM_VERSION),24)
    runner_build_args += -Dfibula.graal.compiler.module=jdk.graal.compiler
    runner_build_args += -Dfibula.graal.compiler.package.prefix=jdk.graal
  endif
endif
ifdef GEN
  runner_build_args += -Dfibula.generate=$(GEN)
  test_args += -Dtest=$(GEN)
endif
ifdef DEBUG_INFO
  runner_build_args += -Dquarkus.native.debug.enabled
endif
ifdef NATIVE_ARGS
  runner_build_args += -Dquarkus.native.additional-build-args=$(NATIVE_ARGS)
endif

common_maven_args =
ifdef QUARKUS_SNAPSHOT
  common_maven_args += -Dquarkus.platform.group-id=io.quarkus
  common_maven_args += -Dquarkus.platform.version=999-SNAPSHOT
  common_maven_args += -Dquarkus-plugin.version=999-SNAPSHOT
endif

ifeq ($(MAVEN_DEBUG),test)
  mvnw += -Dmaven.surefire.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
endif

run: $(samples_runner_jvm) do-run
.PHONY: run

run-native: $(samples_runner_native) do-run
.PHONY: run-native

do-run:
> cd fibula-samples
> $(java) $(system_props) -jar $(benchmarks_jar) $(benchmark_params)
.PHONY: do-run

$(bootstrap_jar): $(shell find . -type f -name "*.java" ! -path "./fibula-it/*" ! -path "./fibula-samples/*" ! -path "./*/target/*")
$(bootstrap_jar): $(shell find . -type f -name "*.json" ! -path "./*/target/*")
$(bootstrap_jar): $(shell find . -type f -name "pom.xml" ! -path "./fibula-it/*" ! -path "./fibula-samples/*" ! -path "./*/target/*")
$(bootstrap_jar): $(shell find . -type f -name "application.properties" ! -path "./fibula-it/*" ! -path "./fibula-samples/*" ! -path "./*/target/*")
$(bootstrap_jar):
> $(mvnw) install $(common_maven_args) -DskipTests --projects !fibula-samples,!fibula-it

$(samples_runner_jvm): $(shell find fibula-samples -type f -name "*.java" ! -path "./*/target/*")
$(samples_runner_jvm): $(shell find fibula-samples -type f -name "pom.xml" ! -path "./*/target/*")
$(samples_runner_jvm): $(shell find fibula-samples -type f -name "application.properties" ! -path "fibula-samples/target/*")
$(samples_runner_jvm): $(bootstrap_jar)
> $(mvnw_runner) package -DskipTests -pl fibula-samples $(runner_build_args)

$(samples_runner_native): $(shell find fibula-samples -type f -name "*.java" ! -path "./*/target/*")
$(samples_runner_native): $(shell find fibula-samples -type f -name "pom.xml" ! -path "./*/target/*")
$(samples_runner_native): $(shell find fibula-samples -type f -name "application.properties" ! -path "fibula-samples/target/*")
$(samples_runner_native): $(bootstrap_jar)
> $(mvnw_runner) package -DskipTests -pl fibula-samples -Pnative $(runner_build_args)

samples: $(samples_runner_jvm)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples

samples-native: $(samples_runner_native)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples-native

$(test_runner_jvm): $(shell find fibula-it -type f -name "*.java" ! -path "./*/target/*")
$(test_runner_jvm): $(shell find fibula-it -type f -name "pom.xml" ! -path "./*/target/*")
$(test_runner_jvm): $(bootstrap_jar)
> $(mvnw_runner) package $(common_maven_args) -DskipTests -pl fibula-it $(runner_build_args)

$(test_runner_native): $(shell find fibula-it -type f -name "*.java" ! -path "./*/target/*")
$(test_runner_native): $(shell find fibula-it -type f -name "pom.xml" ! -path "./*/target/*")
$(test_runner_native): $(bootstrap_jar)
> $(mvnw_runner) package $(common_maven_args) -DskipTests -pl fibula-it -Pnative $(runner_build_args)

test: $(bootstrap_jar) $(test_runner_jvm)
> $(mvnw) $(test_args) -pl fibula-it $(system_props)
.PHONY: test

test-native: $(bootstrap_jar) $(test_runner_native)
> $(mvnw) $(test_args) $(common_maven_args) -pl fibula-it $(system_props)
.PHONY: test-native

clean:
> $(mvnw) clean
.PHONY: clean

QUARKUS_HOME ?= $(HOME)/1/quarkus-quarkus

build-quarkus:
> cd $(QUARKUS_HOME)
> GRADLE_JAVA_HOME=$(HOME)/opt/java-17u-dev JAVA_HOME=$(JAVA_HOME) ./mvnw -Dquickly
.PHONY: build-quarkus

clean-quarkus:
> cd $(QUARKUS_HOME)
> JAVA_HOME=$(JAVA_HOME) ./mvnw clean
.PHONY: clean-quarkus
