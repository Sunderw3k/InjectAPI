mkdir run
cd run

echo "Copying InjectAPI"
cp ../../../build/libs/*-all.jar InjectAPI-all.jar

echo "Compiling App"
kotlinc -include-runtime -d App.jar ../App.kt

echo "Compiling Agent"
kotlinc -include-runtime -cp "App.jar:InjectAPI-all.jar" -d Agent.jar ../Agent.kt

echo "Writing Manifest"
jar ufm Agent.jar ../MANIFEST.MF

echo "Done!"
