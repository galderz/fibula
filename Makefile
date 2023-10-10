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

JAVA_HOME ?= $(HOME)/opt/boot-java-20
#JAVA_HOME ?= $(HOME)/opt/graalvm-community-openjdk-20.0.2+9.1/Contents/Home

java = $(JAVA_HOME)/bin/java
bootstrap_jar = fibula-bootstrap/target/quarkus-app/quarkus-run.jar
samples_jar = fibula-samples/target/quarkus-app/quarkus-run.jar

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
> $(mvnw) install -DskipTests -pl fibula-samples -Dquarkus.package.main-class=bootstrap
> $(java) -jar $(samples_jar)
.PHONY: samples

runner: $(bootstrap_jar)
> $(mvnw) install -DskipTests -pl fibula-samples -Dquarkus.package.main-class=runner
> $(java) -jar $(samples_jar)
.PHONY: runner

$(bootstrap_jar): $(shell find . -type f -name '*.java')
$(bootstrap_jar): $(shell find . -type f -name 'pom.xml')
$(bootstrap_jar):
> $(mvnw) install -DskipTests --projects !fibula-samples

build:
> $(mvnw) -DskipTests=true install -Dquarkus.package.quiltflower.enabled=true
.PHONY: build

core-dev: build
> $(mvnw) clean quarkus:dev -pl $(core_dir)
.PHONY: core-dev

runner-dev: build
> $(mvnw) clean quarkus:dev -pl $(runner_dir) -Dquarkus.package.quiltflower.enabled=true
.PHONY: runner-dev

native: build
> $(mvnw) package -Pnative -pl $(runner_dir)
.PHONY: native

runner-package: $(deployment_jar) $(annotations_jar) $(results_jar)
> $(mvnw) package -pl $(runner_dir) -Dquarkus.package.quiltflower.enabled=true
.PHONY: runner-package

$(core_jar): $(annotations_jar)
$(core_jar): $(runtime_jar)
$(core_jar): $(deployment_jar)
$(core_jar): $(shell find $(core_dir)/src -type f -name '*.java')
> $(mvnw) package -DskipTests -pl $(core_dir)

$(deployment_jar): $(annotations_jar)
$(deployment_jar): $(runtime_jar)
$(deployment_jar): $(deployment_pom)
$(deployment_jar): $(shell find $(deployment_dir)/src -type f -name '*.java')
> $(mvnw) package -DskipTests -pl $(deployment_dir)

$(results_jar): $(results_pom)
$(results_jar): $(shell find $(results_dir)/src -type f -name '*.java')
> $(mvnw) package -DskipTests -pl $(results_dir)

$(runtime_jar): $(runtime_pom)
$(runtime_jar): $(shell find $(runtime_dir)/src -type f -name '*.java')
> $(mvnw) package -DskipTests -pl $(runtime_dir)

$(annotations_jar): $(annotations_pom)
$(annotations_jar): $(shell find $(annotations_dir)/src -type f -name '*.java')
> $(mvnw) package -DskipTests -pl $(annotations_dir)

clean:
> $(mvnw) clean
.PHONY: clean
