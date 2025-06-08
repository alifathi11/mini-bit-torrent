#!/bin/bash
set -e

# Clean and create output directory
rm -rf out_tracker
mkdir -p out_tracker

echo "Java version: $(javac -version)"

# Compile all tracker and common java files
javac -cp "lib/*" -d out_tracker $(find ./tracker -name "*.java") $(find ./common -name "*.java")

# Extract all libraries from lib directory
mkdir -p temp_lib
cd temp_lib
for f in ../lib/*.jar; do
    jar xf "$f"
done
cp -r * ../out_tracker/
cd ..
rm -rf temp_lib

# Create manifest file
echo "Main-Class: tracker.TrackerMain" > manifest_tracker.txt

# Create the JAR file
jar cvfm tracker.jar manifest_tracker.txt -C out_tracker .  >/dev/null

# Clean up
rm manifest_tracker.txt

echo "Tracker JAR created successfully: tracker.jar"

rm -rf out_tracker

