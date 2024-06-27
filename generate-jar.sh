#!/bin/bash -ex

javac MouseIdleStats.java
# echo "Main-Class: MouseIdleStats" > Manifest.txt
jar cvfm mouseIdleStats.jar Manifest.txt MouseIdleStats*.class
mkdir -p dist
mv MouseIdleStats*.class dist
mv mouseIdleStats.jar dist