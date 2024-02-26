#!/bin/bash -e

javac MouseMover.java
echo "Main-Class: MouseMover" > Manifest.txt
jar cvfm mouseMover.jar Manifest.txt MouseMover*.class
