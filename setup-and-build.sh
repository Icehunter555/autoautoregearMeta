#!/bin/bash

echo "========================================"
echo "Meta Mod - Setup and Build Script"
echo "========================================"
echo ""

echo "Step 1: Moving source files to proper directory..."
echo ""

mkdir -p "src/main/kotlin/dev/wizard/meta"

echo "Moving Kotlin files..."
for file in *.kt; do
    if [ -f "$file" ]; then
        mv "$file" "src/main/kotlin/dev/wizard/meta/"
        echo "  Moved $file"
    fi
done

echo "Moving directories..."
for dir in command event graphics gui manager mixins module setting structs translation util; do
    if [ -d "$dir" ]; then
        rm -rf "src/main/kotlin/dev/wizard/meta/$dir"
        mv "$dir" "src/main/kotlin/dev/wizard/meta/"
        echo "  Moved $dir/"
    fi
done

echo ""
echo "Step 2: Making gradlew executable..."
echo ""

chmod +x gradlew

echo "Step 3: Setting up decompiled workspace..."
echo "This may take several minutes on first run..."
echo ""

./gradlew setupDecompWorkspace

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Setup failed!"
    exit 1
fi

echo ""
echo "Step 4: Building the mod..."
echo ""

./gradlew build

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Build failed!"
    exit 1
fi

echo ""
echo "========================================"
echo "Build completed successfully!"
echo "========================================"
echo ""
echo "The mod JAR file is located at:"
echo "build/libs/meta-0.3B-10mq29.jar"
echo ""
echo "To run the mod in development:"
echo "  ./gradlew runClient"
echo ""
