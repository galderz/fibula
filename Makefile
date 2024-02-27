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

bootstrap_jar = fibula-bootstrap/target/quarkus-app/quarkus-run.jar
java = $(JAVA_HOME)/bin/java
runner_jvm = fibula-samples/target/runner-jvm/quarkus-run.jar
runner_native = fibula-samples/target/runner-native/fibula-samples-1.0.0-SNAPSHOT-runner
samples_jar = fibula-samples/target/fibula-samples-1.0.0-SNAPSHOT.jar

system_props =
ifdef LOG_LEVEL
  ifeq ($(LOG_LEVEL),DEBUG)
    system_props += -Dquarkus.log.category.\"org.mendrugo.fibula\".level=DEBUG
  endif
endif

# Benchmark name
benchmark_params += $(BENCHMARK)
# Measurement forks
benchmark_params += -f
benchmark_params += 2
# Measurement iterations
benchmark_params += -i
benchmark_params += 2
# Warmup iterations
benchmark_params += -wi
benchmark_params += 2
# Measurement time
benchmark_params += -r
benchmark_params += 2
# Warmup time
benchmark_params += -w
benchmark_params += 2

ifdef RESULT_FORMAT
  benchmark_params += -rf
  benchmark_params += $(RESULT_FORMAT)
endif

mvnw += JAVA_HOME=$(JAVA_HOME)
mvnw_runner += JAVA_HOME=$(JAVA_HOME)
ifdef DEBUG_IDE
  mvnw_runner += $(HOME)/opt/maven/bin/mvnDebug
else
  mvnw_runner += $(HOME)/opt/maven/bin/mvn
endif

mvnw += $(HOME)/opt/maven/bin/mvn

ifdef VERBOSE
  mvnw_runner += -X
endif

ifdef DEBUG
  java += -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000
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

test_args = test
ifdef TEST
  test_args += -Dtest=$(TEST)
endif

run: $(runner_jvm) do-run
.PHONY: run

run-native: $(runner_native) do-run
.PHONY: run-native

do-run:
> cd fibula-samples
> $(java) $(system_props) -jar target/bootstrap/quarkus-run.jar $(benchmark_params)
.PHONY: do-run

$(bootstrap_jar): $(shell find . -path ./fibula-it -prune -o -name '*.java' -print)
$(bootstrap_jar): $(shell find . -path ./fibula-it -prune -o -name '*.json' -print)
$(bootstrap_jar): $(shell find . -path ./fibula-it -prune -o -name '*.xml' -print)
$(bootstrap_jar):
> $(mvnw) install -DskipTests --projects !fibula-samples,!fibula-it

$(runner_jvm): $(bootstrap_jar) $(samples_jar)
> $(mvnw_runner) package -DskipTests -pl fibula-samples -Prunner-jvm $(runner_build_args)

$(runner_native): $(bootstrap_jar) $(samples_jar)
> $(mvnw_runner) package -DskipTests -pl fibula-samples -Prunner-native $(runner_build_args)

$(samples_jar): $(shell find . -path ./fibula-it -prune -o -name '*.java' -print)
$(samples_jar): $(shell find . -path ./fibula-it -prune -o -name '*.json' -print)
$(samples_jar): $(shell find . -path ./fibula-it -prune -o -name '*.xml' -print)
$(samples_jar):
> $(mvnw) package -DskipTests -pl fibula-samples

samples: $(bootstrap_jar) $(runner_jvm)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples

samples-native: $(bootstrap_jar) $(runner_native)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples-native

test: $(bootstrap_jar) $(runner_jvm)
> $(mvnw) $(test_args) -pl fibula-it
.PHONY: test

clean:
> $(mvnw) clean
.PHONY: clean
