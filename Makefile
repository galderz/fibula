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
JAVA_HOME ?= $(HOME)/opt/graal-21
VERSION ?= 999-SNAPSHOT
MAVEN_HOME ?= $(HOME)/opt/maven

benchmarks_jar = target/benchmarks.jar
final_jar = fibula-samples/target/fibula-samples-$(VERSION).jar
java = $(JAVA_HOME)/bin/java
samples_runner = fibula-samples/target/benchmarks

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
mvnw_runner += JAVA_HOME=$(JAVA_HOME)
ifeq ($(MAVEN_DEBUG),process)
  mvnw_runner += $(MAVEN_HOME)/bin/mvnDebug
else
  mvnw_runner += $(MAVEN_HOME)/bin/mvn
endif

mvnw += $(MAVEN_HOME)/bin/mvn

ifdef MAVEN_VERBOSE
  mvnw_runner += -X
  mvnw += -X
endif

ifdef DEBUG
  java += -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000
  benchmark_params += -jvmArgs
  benchmark_params += ""
endif

test_args =
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
ifdef TEST
  test_args += -Dtest=$(TEST)
endif
ifdef DEBUG_INFO
  runner_build_args += -Ddebug
  # todo add option to add -H:+SourceLevelDebug
  runner_build_args += -DbuildArgs=-H:-DeleteLocalSymbols
endif
ifdef NATIVE_ARGS
  runner_build_args += -DbuildArgs=$(NATIVE_ARGS)
endif

ifeq ($(MAVEN_DEBUG),test)
  mvnw += -Dmaven.surefire.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
else ifeq ($(MAVEN_DEBUG),test-native)
  mvnw += -Dmaven.failsafe.debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
endif

run: $(samples_runner) do-run
.PHONY: run

# Touch jar file in case there's no rebuild and surefire wrongly tries to execute native tests
test:
> touch fibula-it/target/benchmarks.jar || true
> $(mvnw) verify $(test_args) -pl fibula-it -am
.PHONY: test

run-jvm: $(final_jar) do-run
.PHONY: run

test-jvm:
> $(mvnw) test $(test_args) -pl fibula-it -am -Djvm.mode
.PHONY: test-jvm

do-run:
> cd fibula-samples
> $(java) -jar $(benchmarks_jar) $(benchmark_params)
.PHONY: do-run

$(final_jar): $(shell find . -type f -name "*.java" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "*.json" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "pom.xml" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "application.properties" ! -path "./*/target/*")
$(final_jar):
> $(mvnw_runner) install -DskipTests -e -Djvm.mode
> touch $@

$(samples_runner): $(shell find fibula-samples -type f -name "*.java" ! -path "./*/target/*")
$(samples_runner): $(shell find fibula-samples -type f -name "pom.xml" ! -path "./*/target/*")
$(samples_runner): $(shell find fibula-samples -type f -name "application.properties" ! -path "fibula-samples/target/*")
$(samples_runner): $(final_jar)
> $(mvnw_runner) package -pl fibula-samples $(runner_build_args)

clean:
> $(mvnw) clean
.PHONY: clean
