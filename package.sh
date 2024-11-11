#!/bin/bash

# Define source and destination directories
PROJECT_NAME="MouseIdleStats"
PROJECT_VERSION="1.0.0"
ROOT_PACKAGE="br"
MAIN_CLASS="br.com.armandoalmeida.MouseIdleStats"
DEST_DIR="dist/$ROOT_PACKAGE"

printf "\n-- %s (%s) --\n\n" $PROJECT_NAME $PROJECT_VERSION

printf "Cleaning destination directory '%s'... " $DEST_DIR
if [ -d "$DEST_DIR" ]; then
  rm -r "$DEST_DIR"
fi
mkdir -p "$DEST_DIR"
printf "done\n"

CURRENT_JAVA=$(java --version)
printf "\nCurrent java version: %s\n" "$CURRENT_JAVA"

printf "\nCompiling java files in '%s'... " $ROOT_PACKAGE
find "$ROOT_PACKAGE" -name "*.java" | while read -r file; do
  javac "$file"
done
printf "done\n"

printf "Creating jar package... "
# Find and move .class files while preserving the directory structure
find "$ROOT_PACKAGE" -name "*.class" | while read -r file; do
  # Get the directory structure relative to $ROOT_PACKAGE
  relative_path="${file#$ROOT_PACKAGE/}"
  # Create the corresponding directory in $DEST_DIR
  mkdir -p "$DEST_DIR/$(dirname "$relative_path")"
  # Move the .class file to the new location
  mv "$file" "$DEST_DIR/$relative_path"
done

JAR_FILE="dist/$PROJECT_NAME-$PROJECT_VERSION.jar"
echo "Main-Class: $MAIN_CLASS" >MANIFEST.MF
jar cfm "$JAR_FILE" MANIFEST.MF -C dist .
rm -rf MANIFEST.MF
printf "done\n"

EXEC_COMMAND="java -jar $JAR_FILE"
printf "\nRunning: %s\n\n" "$EXEC_COMMAND"

eval "$EXEC_COMMAND"