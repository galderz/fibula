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

annotations_dir = $(annotations_name)
annotations_name = fibula-annotations
annotations_jar = $(annotations_target)/$(annotations_name)-$(version).jar
annotations_pom = $(annotations_dir)/pom.xml
annotations_target = $(annotations_dir)/target

core_dir = $(core_name)
core_name = fibula-core
core_jar = $(core_target)/$(core_name)-$(version).jar
core_pom = $(core_dir)/pom.xml
core_target = $(core_dir)/target

deployment_dir = fibula-extension/deployment
deployment_name = fibula-extension-deployment
deployment_jar = $(deployment_target)/$(deployment_name)-$(version).jar
deployment_pom = $(deployment_dir)/pom.xml
deployment_target = $(deployment_dir)/target

runtime_dir = fibula-extension/runtime
runtime_name = fibula-extension
runtime_jar = $(runtime_target)/$(runtime_name)-$(version).jar
runtime_pom = $(runtime_dir)/pom.xml
runtime_target = $(runtime_dir)/target

mvnw += JAVA_HOME=$(JAVA_HOME)
mvnw += ./mvnw
version := 1.0.0-SNAPSHOT

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

$(runtime_jar): $(runtime_pom)
$(runtime_jar): $(shell find $(runtime_dir)/src -type f -name '*.java')
> $(mvnw) package -DskipTests -pl $(runtime_dir)

$(annotations_jar): $(annotations_pom)
$(annotations_jar): $(shell find $(annotations_dir)/src -type f -name '*.java')
> $(mvnw) package -DskipTests -pl $(annotations_dir)

clean:
> $(mvnw) clean
.PHONY: clean
