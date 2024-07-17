mkdir run
cd run

echo "Copying InjectAPI"
cp ../../../build/libs/*-all.jar InjectAPI-all.jar

echo "Compiling App"
javac -d . ../App.java
jar -cf App.jar App.class

echo "Compiling Agent"
kotlinc -include-runtime -cp "App.jar:InjectAPI-all.jar" -d Agent.jar ../Agent.kt

echo "Writing Manifest"
jar ufm Agent.jar ../MANIFEST.MF

echo "Done!"
