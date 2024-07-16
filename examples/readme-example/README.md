## README Example

This is a slightly modified version of the example in [README.md](../../README.md)

### Running

The code to run this example is bundled in [setup.sh](setup.sh).
The commands to run are:

```shell
# Or wherever the latest far jar is
cp ../../build/libs/*-all.jar InjectAPI-all.jar

# Compile the App to inject into
kotlinc -include-runtime -d App.jar App.kt

# Compile Agent. Depends on App for the call to App::class.java
kotlinc -include-runtime -cp "App.jar:InjectAPI-all.jar" -d Agent.jar Agent.kt

# Add MANIFEST.MF to the Agent jar
jar ufm Agent.jar MANIFEST.MF

# Run App.jar with a javaagent attached. Can also be attached at runtime
# For runtime Agent.kt needs to use agentmain instead of premain
java -javaagent:Agent.jar App.jar
```