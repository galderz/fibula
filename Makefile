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
JAVA_HOME ?= $(HOME)/opt/java-21
VERSION ?= 999-SNAPSHOT
MAVEN_HOME ?= $(HOME)/opt/maven

benchmarks_jar = target/benchmarks.jar
final_jar = fibula-generator/target/fibula-generator-$(VERSION).jar
it_runner = fibula-it/target/fibula-it-$(VERSION)-runner
java = $(JAVA_HOME)/bin/java
samples_runner = fibula-samples/target/benchmarks

system_props =
ifdef LOG_LEVEL
  ifeq ($(LOG_LEVEL),DEBUG)
    system_props += -Dquarkus.log.category.\"org.mendrugo.fibula\".level=DEBUG
  endif
endif

PROF ?=

ifdef PROF
  MEASURE_FORKS ?= 1
  MEASURE_IT ?= 1
  MEASURE_TIME ?= 10
  WARMUP_IT ?= 1
  WARMUP_TIME ?= 10
else
  MEASURE_FORKS ?= 1
  MEASURE_IT ?= 1
  MEASURE_TIME ?= 1
  WARMUP_IT ?= 1
  WARMUP_TIME ?= 1
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
benchmark_params += -foe
benchmark_params += true
benchmark_params += -v
benchmark_params += EXTRA

ifdef RESULT_FORMAT
  benchmark_params += -rf
  benchmark_params += $(RESULT_FORMAT)
endif

ifdef PROF
  benchmark_params += -prof
  benchmark_params += $(PROF)
endif

MAVEN_DEBUG ?=

mvnw += JAVA_HOME=$(JAVA_HOME)
mvnw += GRAALVM_HOME=$(GRAALVM_HOME)
mvnw_runner += JAVA_HOME=$(JAVA_HOME)
mvnw_runner += GRAALVM_HOME=$(GRAALVM_HOME)
ifeq ($(MAVEN_DEBUG),process)
  mvnw_runner += $(MAVEN_HOME)/bin/mvnDebug
else
  mvnw_runner += $(MAVEN_HOME)/bin/mvn
endif

mvnw += $(MAVEN_HOME)/bin/mvn

ifdef MAVEN_VERBOSE
  mvnw_runner += -X
endif

ifdef DEBUG
  java += -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000
  benchmark_params += -jvmArgs
  benchmark_params += ""
endif

test_args += test
ifdef TEST
  test_args += -Dtest=$(TEST)
endif

ifdef NATIVE_AGENT
  java += -Dfibula.native.agent=true
  test_args += -Dfibula.native.agent=true
endif

ifdef RUNNER_DEBUG
  java += -Dfibula.runner.debug=true
  test_args += -Dfibula.runner.debug=true
endif

runner_build_args =
ifdef DECOMPILE
  runner_build_args += -Dquarkus.package.jar.decompiler.enabled
endif
ifdef REPORTS
  runner_build_args += -Dquarkus.native.enable-reports
endif
ifdef GEN
  runner_build_args += -Dfibula.generate=$(GEN)
  test_args += -Dtest=$(GEN)
endif
ifdef DEBUG_INFO
  runner_build_args += -Dquarkus.native.debug.enabled
  # todo add option to add -H:+SourceLevelDebug
  runner_build_args += -Dfibula.native.additional-build-args=-H:-DeleteLocalSymbols
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

run-native: $(samples_runner) do-run
.PHONY: run-native

run: $(final_jar) do-run
.PHONY: run

do-run:
> cd fibula-samples
> $(java) $(system_props) -jar $(benchmarks_jar) $(benchmark_params)
.PHONY: do-run

$(final_jar): $(shell find . -type f -name "*.java" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "*.json" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "pom.xml" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "application.properties" ! -path "./*/target/*")
$(final_jar):
> $(mvnw_runner) install $(common_maven_args) -DskipTests
> touch $@

$(samples_runner): $(shell find fibula-samples -type f -name "*.java" ! -path "./*/target/*")
$(samples_runner): $(shell find fibula-samples -type f -name "pom.xml" ! -path "./*/target/*")
$(samples_runner): $(shell find fibula-samples -type f -name "application.properties" ! -path "fibula-samples/target/*")
$(samples_runner): $(final_jar)
> $(mvnw_runner) package -DskipTests -pl fibula-samples -Pnative $(runner_build_args)

samples: $(final_jar)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples

samples-native: $(samples_runner)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples-native

$(it_runner): $(shell find fibula-it/src -type f -name "*.java" ! -path "./*/target/*")
$(it_runner): $(shell find fibula-it -type f -name "pom.xml" ! -path "./*/target/*")
$(it_runner): $(final_jar)
> $(mvnw_runner) package $(common_maven_args) -DskipTests -pl fibula-it -Pnative $(runner_build_args)

test: $(final_jar)
> $(mvnw) $(test_args) -pl fibula-it $(system_props)
.PHONY: test

test-native: $(it_runner)
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
