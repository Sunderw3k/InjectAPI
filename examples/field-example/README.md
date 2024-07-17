## Field Example

This example is a showcase of field access redirection.

The [App](App.java) is written in java due to how kotlin handles fields.

### Explanation

You can hook on gets and sets, if you need to modify a value
of a field before its accessed by some method, you can use a get hook,
which looks like this:
```kotlin
doStuff(hook(this.field, /* extra local parameters */))
```

A set hook looks like this:
```kotlin
this.field = hook(doOtherStuff(3), /* extra locals */)
```

### Running

The code to run this example is bundled in [setup.sh](setup.sh).
The commands to run are:

```shell
# Or wherever the latest far jar is
cp ../../build/libs/*-all.jar InjectAPI-all.jar

# Compile the App to inject into, also make a jar from it
javac -d . App.java
jar -cf App.jar App.class

# Compile Agent. Depends on App for the call to App::class.java
kotlinc -include-runtime -cp "App.jar:InjectAPI-all.jar" -d Agent.jar Agent.kt

# Add MANIFEST.MF to the Agent jar
jar ufm Agent.jar MANIFEST.MF

# Run App with a javaagent attached. Can also be attached at runtime
# For runtime Agent.kt needs to use agentmain instead of premain
java -javaagent:Agent.jar App
```