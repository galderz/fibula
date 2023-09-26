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

#JAVA_HOME ?= $(HOME)/opt/boot-java-20
JAVA_HOME ?= $(HOME)/opt/graalvm-community-openjdk-20.0.2+9.1/Contents/Home

#annotations_dir = $(annotations_name)
#annotations_name = fibula-annotations
#annotations_jar = $(annotations_target)/$(annotations_name)-$(version).jar
#annotations_pom = $(annotations_dir)/pom.xml
#annotations_target = $(annotations_dir)/target

core_dir = $(core_name)
core_name = fibula-core
#core_jar = $(core_target)/$(core_name)-$(version).jar
#core_pom = $(core_dir)/pom.xml
#core_target = $(core_dir)/target
#
#deployment_dir = fibula-extension/deployment
#deployment_name = fibula-extension-deployment
#deployment_jar = $(deployment_target)/$(deployment_name)-$(version).jar
#deployment_pom = $(deployment_dir)/pom.xml
#deployment_target = $(deployment_dir)/target
#
#results_dir = $(results_name)
#results_name = fibula-results
#results_jar = $(results_target)/$(results_name)-$(version).jar
#results_pom = $(results_dir)/pom.xml
#results_target = $(results_dir)/target

runner_dir = $(runner_name)
runner_name = fibula-runner
#runner_jar = $(runner_target)/$(runner_name)-$(version).jar
#runner_pom = $(runner_dir)/pom.xml
#runner_target = $(runner_dir)/target
#
#runtime_dir = fibula-extension/runtime
#runtime_name = fibula-extension
#runtime_jar = $(runtime_target)/$(runtime_name)-$(version).jar
#runtime_pom = $(runtime_dir)/pom.xml
#runtime_target = $(runtime_dir)/target

mvnw += JAVA_HOME=$(JAVA_HOME)
ifdef DEBUG_IDE
  mvnw += $(HOME)/opt/maven/bin/mvnDebug
else
  mvnw += ./mvnw
endif

version := 1.0.0-SNAPSHOT

ifdef DEBUG
  mvnw += -X
endif

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
