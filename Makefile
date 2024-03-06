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
samples_runner_jvm = fibula-samples/target/runner-jvm/quarkus-run.jar
samples_runner_native = fibula-samples/target/runner-native/fibula-samples-1.0.0-SNAPSHOT-runner
samples_jar = fibula-samples/target/fibula-samples-1.0.0-SNAPSHOT.jar
test_jar = fibula-it/target/fibula-it-1.0.0-SNAPSHOT.jar
test_runner_jvm = fibula-it/target/runner-jvm/quarkus-run.jar
test_runner_native = fibula-it/target/runner-jvm/runner-native/fibula-it-1.0.0-SNAPSHOT-runner

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

common_maven_args =
ifdef QUARKUS_SNAPSHOT
  common_maven_args += -Dquarkus.platform.group-id=io.quarkus
  common_maven_args += -Dquarkus.platform.version=999-SNAPSHOT
  common_maven_args += -Dquarkus-plugin.version=999-SNAPSHOT
endif

run: $(samples_runner_jvm) do-run
.PHONY: run

run-native: $(samples_runner_native) do-run
.PHONY: run-native

do-run:
> cd fibula-samples
> $(java) $(system_props) -jar ../$(bootstrap_jar) $(benchmark_params)
.PHONY: do-run

$(bootstrap_jar): $(shell find . -path ./fibula-it -prune -o -name '*.java' -print)
$(bootstrap_jar): $(shell find . -path ./fibula-it -prune -o -name '*.json' -print)
$(bootstrap_jar): $(shell find . -path ./fibula-it -prune -o -name 'pom.xml' -print)
$(bootstrap_jar): $(shell find . -path ./fibula-it -prune -o -name 'application.properties' -print)
$(bootstrap_jar):
> $(mvnw) install $(common_maven_args) -DskipTests --projects !fibula-samples,!fibula-it

$(samples_runner_jvm): $(bootstrap_jar) $(samples_jar)
> $(mvnw_runner) package -DskipTests -pl fibula-samples -Prunner-jvm $(runner_build_args)

$(samples_runner_native): $(bootstrap_jar) $(samples_jar)
> $(mvnw_runner) package -DskipTests -pl fibula-samples -Prunner-native $(runner_build_args)

$(samples_jar): $(shell find . -path ./fibula-it -prune -o -name '*.java' -print)
$(samples_jar): $(shell find . -path ./fibula-it -prune -o -name '*.json' -print)
$(samples_jar): $(shell find . -path ./fibula-it -prune -o -name 'pom.xml' -print)
$(samples_jar): $(shell find . -path ./fibula-it -prune -o -name 'application.properties' -print)
$(samples_jar):
> $(mvnw) package -DskipTests -pl fibula-samples

samples: $(samples_runner_jvm)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples

samples-native: $(samples_runner_native)
> $(mvnw) $(test_args) -pl fibula-samples -Dfibula.test.quick
.PHONY: samples-native

$(test_jar): $(shell find fibula-it -name '*.java' -print)
$(test_jar): $(shell find fibula-it -name 'pom.xml' -print)
$(test_jar):
> $(mvnw) package $(common_maven_args) -DskipTests -pl fibula-it

$(test_runner_jvm): $(bootstrap_jar) $(test_jar)
> $(mvnw_runner) package $(common_maven_args) -DskipTests -pl fibula-it -Prunner-jvm $(runner_build_args)

$(test_runner_native): $(bootstrap_jar) $(test_jar)
> $(mvnw_runner) package $(common_maven_args) -DskipTests -pl fibula-it -Prunner-native $(runner_build_args)

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
