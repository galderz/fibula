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

JAVA_HOME ?= $(HOME)/opt/java-21
GRAALVM_HOME ?= $(HOME)/opt/graal-21
MODE ?= jvm

bootstrap_jar = fibula-bootstrap/target/quarkus-app/quarkus-run.jar
benchmark_params += -i
benchmark_params += 2
benchmark_params += -f
benchmark_params += 2
benchmark_params += -p
benchmark_params += fibula.package.mode=$(MODE)
java = $(JAVA_HOME)/bin/java
samples_bootstrap_jar = fibula-samples/target/quarkus-app/quarkus-run.jar
samples_runner_jar = fibula-samples/target/runner-app/quarkus-run.jar

mvnw += JAVA_HOME=$(JAVA_HOME)
ifdef DEBUG_IDE
  mvnw += $(HOME)/opt/maven/bin/mvnDebug
else
  mvnw += ./mvnw
endif

ifdef DEBUG
  mvnw += -X
endif

samples: $(bootstrap_jar)
> $(mvnw) package -DskipTests -pl fibula-samples
> GRAALVM_HOME=$(GRAALVM_HOME) $(java) -jar $(samples_bootstrap_jar) $(benchmark_params)
.PHONY: samples

runner: $(bootstrap_jar)
> $(mvnw) package -DskipTests -pl fibula-samples -Prunner
> $(java) -jar $(samples_runner_jar)
.PHONY: runner

native: $(bootstrap_jar)
> $(mvnw) package -DskipTests -pl fibula-samples
> GRAALVM_HOME=$(GRAALVM_HOME) $(java) -jar $(samples_bootstrap_jar) $(benchmark_params)
.PHONY: native

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
