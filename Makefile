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
GRAALVM_EE_HOME ?= $(HOME)/opt/ee-graal-21
VERSION ?= 999-SNAPSHOT
MAVEN_HOME ?= $(HOME)/opt/maven

benchmarks_jar = target/benchmarks.jar
final_jar = fibula-generator/target/fibula-generator-$(VERSION).jar
java_ee = $(GRAALVM_EE_HOME)/bin/java
java = $(JAVA_HOME)/bin/java
jdwp = -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*
jdwp_fork_port = 6006
jdwp_runner_port = 5005
maven_surefire_debug = -Dmaven.surefire.debug="$(jdwp):$(jdwp_runner_port)"
maven_failsafe_debug = -Dmaven.failsafe.debug="$(jdwp):$(jdwp_runner_port)"
samples_jar = fibula-samples/target/benchmarks.jar
samples_runner = fibula-samples/target/benchmarks
samples_pgo_runner = fibula-samples/target/benchmarks.output/default/benchmarks

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

ifdef TU
  benchmark_params += -tu
  benchmark_params += $(TU)
else
  benchmark_params += -tu
  benchmark_params += us
endif

ifdef VERBOSE
  benchmark_params += -v
  benchmark_params += EXTRA
endif

ifdef RESULT_FORMAT
  benchmark_params += -rf
  benchmark_params += $(RESULT_FORMAT)
endif

ifdef PROF
  benchmark_params += -prof
  benchmark_params += $(PROF)
endif

DEBUG ?=

mvnw += JAVA_HOME=$(JAVA_HOME)
ifeq ($(DEBUG),maven)
  mvnw += $(MAVEN_HOME)/bin/mvnDebug
else
  mvnw += $(MAVEN_HOME)/bin/mvn
endif

ifdef MAVEN_VERBOSE
  mvnw += -X
endif

ifeq ($(DEBUG),runner)
  java += $(jdwp):$(jdwp_runner_port)
  benchmark_params += -jvmArgs
  benchmark_params += ""
else ifeq ($(DEBUG),fork)
  benchmark_params += -jvmArgs
  benchmark_params += "$(jdwp):$(jdwp_fork_port)"
endif

test_args =
ifdef TEST
  test_args += -Dtest=$(TEST)
  test_args += -Dit.test=$(TEST)
endif

ifdef NATIVE_AGENT
  java += -Dfibula.native.agent=true
  test_args += -Dfibula.native.agent=true
endif

runner_build_args =
ifdef DEBUG_INFO
  runner_build_args += -Ddebug
  # todo add option to add -H:+SourceLevelDebug
  runner_build_args += -DbuildArgs=-H:-DeleteLocalSymbols
endif
ifdef NATIVE_ARGS
  runner_build_args += -DbuildArgs=$(NATIVE_ARGS)
endif

run: $(samples_runner) do-run
.PHONY: run

# Touch jar file in case there's no rebuild and surefire wrongly tries to execute native tests
test:
> touch fibula-it/target/benchmarks.jar || true
ifeq ($(DEBUG),runner)
> $(mvnw) $(maven_failsafe_debug) verify $(test_args) -pl fibula-it -am
else
> $(mvnw) verify $(test_args) -pl fibula-it -am
endif
.PHONY: test

run-jvm: $(samples_jar) do-run
.PHONY: run-jvm

test-jvm:
> touch fibula-it/target/benchmarks.jar || true
ifeq ($(DEBUG),runner)
> $(mvnw) $(maven_surefire_debug) test $(test_args) -pl fibula-it -am -Djvm.mode
endif
> $(mvnw) test $(test_args) -pl fibula-it -am -Djvm.mode
.PHONY: test-jvm

do-run:
> cd fibula-samples
> $(java) -jar $(benchmarks_jar) $(benchmark_params)
.PHONY: do-run

run-pgo: $(samples_pgo_runner) do-run-pgo
.PHONY: run-pgo

do-run-pgo:
> cd fibula-samples
> $(java_ee) -jar $(benchmarks_jar) $(benchmark_params)
.PHONY: do-run-pgo

$(final_jar): $(shell find . -type f -name "*.java" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "*.json" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "pom.xml" ! -path "./*/target/*")
$(final_jar): $(shell find . -type f -name "application.properties" ! -path "./*/target/*")
$(final_jar):
> $(mvnw) install -DskipTests -e -pl !fibula-it,!fibula-samples
> touch $@

$(samples_jar): $(shell find fibula-samples -type f -name "*.java" ! -path "./*/target/*")
$(samples_jar): $(shell find fibula-samples -type f -name "pom.xml" ! -path "./*/target/*")
$(samples_jar): $(shell find fibula-samples -type f -name "application.properties" ! -path "fibula-samples/target/*")
$(samples_jar): $(final_jar)
> $(mvnw) package -pl fibula-samples $(runner_build_args) -Djvm.mode

$(samples_runner): $(shell find fibula-samples -type f -name "*.java" ! -path "./*/target/*")
$(samples_runner): $(shell find fibula-samples -type f -name "pom.xml" ! -path "./*/target/*")
$(samples_runner): $(shell find fibula-samples -type f -name "application.properties" ! -path "fibula-samples/target/*")
$(samples_runner): $(final_jar)
> $(mvnw) package -pl fibula-samples $(runner_build_args)

$(samples_pgo_runner): $(shell find fibula-samples -type f -name "*.java" ! -path "./*/target/*")
$(samples_pgo_runner): $(shell find fibula-samples -type f -name "pom.xml" ! -path "./*/target/*")
$(samples_pgo_runner): $(shell find fibula-samples -type f -name "application.properties" ! -path "fibula-samples/target/*")
$(samples_pgo_runner): $(final_jar)
> GRAALVM_HOME=$(GRAALVM_EE_HOME) $(mvnw) package -pl fibula-samples $(runner_build_args) -Dpgo

clean:
> $(mvnw) clean
.PHONY: clean
