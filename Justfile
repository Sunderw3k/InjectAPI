LIBRARY_VERSION := "0.6.0"
LIBRARY_PATH :=  justfile_directory() + "/build/libs/InjectAPI-" + LIBRARY_VERSION + "-all.jar"
MANIFEST_PATH :=  justfile_directory() + "/tests/MANIFEST.MF"

build test_name:
	#!/usr/bin/env bash
	cd tests/{{test_name}} 

	rm -r run
	mkdir run
	cd run

	cp "{{LIBRARY_PATH}}" InjectAPI-all.jar

	javac -g:none -d . ../App.java
	jar -cf App.jar App.class

	kotlinc -jvm-target 21 -include-runtime -cp "InjectAPI-all.jar:App.jar" -d Agent.jar ../Agent.kt
	jar ufm Agent.jar "{{MANIFEST_PATH}}"

test test_name: 
	#!/usr/bin/env bash
	cd tests/{{test_name}}/run || exit
	cp "{{LIBRARY_PATH}}" InjectAPI-all.jar

	java -cp InjectAPI-all.jar:App.jar -javaagent:Agent.jar App
	return_code=$?

	if [ "$return_code" -eq 0 ]; then
	  echo "[{{test_name}}] SUCCESS!"
	else
	  echo "[{{test_name}}] FAILED! Code: $return_code"
	fi

test_all:
	@ls tests | grep -v "\..*" | parallel "just test"

build_all:
	@ls tests | grep -v "\..*" | parallel "just build"
