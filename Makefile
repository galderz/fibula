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
MODE ?= jvm

bootstrap_jar = fibula-bootstrap/target/quarkus-app/quarkus-run.jar
java = $(GRAALVM_HOME)/bin/java
samples_bootstrap_jar = target/bootstrap/quarkus-run.jar

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

runner_build_args += -Prunner-$(MODE)
ifdef DECOMPILE
  runner_build_args += -Dquarkus.package.vineflower.enabled=true
endif
ifdef GRAALVM_VERSION
  ifeq ($(GRAALVM_VERSION),24)
    runner_build_args += -Dfibula.graal.compiler.module=jdk.graal.compiler
    runner_build_args += -Dfibula.graal.compiler.package.prefix=jdk.graal
  endif
endif

samples: $(bootstrap_jar)
> cd fibula-samples
> $(mvnw) package
> $(mvnw_runner) package $(runner_build_args)
> $(java) $(system_props) -jar $(samples_bootstrap_jar) $(benchmark_params)
.PHONY: samples

$(bootstrap_jar): $(shell find . -type f -name '*.java')
$(bootstrap_jar): $(shell find . -type f -name 'pom.xml')
$(bootstrap_jar):
> $(mvnw) install -DskipTests --projects !fibula-samples

build:
> $(mvnw) -DskipTests=true install -Dquarkus.package.quiltflower.enabled=true
.PHONY: build

clean:
> $(mvnw) clean
.PHONY: clean
