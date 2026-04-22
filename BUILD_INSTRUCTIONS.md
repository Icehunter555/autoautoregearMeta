# Build Instructions for Meta Mod

## Prerequisites

1. **Java Development Kit (JDK) 8**
   - Download from: https://adoptium.net/temurin/releases/?version=8
   - Make sure `JAVA_HOME` environment variable is set
   - Verify installation: `java -version` (should show 1.8.x)

2. **Internet Connection**
   - Required for downloading Gradle dependencies and Minecraft/Forge files

## Quick Start (Recommended)

### Windows

1. Open Command Prompt or PowerShell in the project directory
2. Run the setup and build script:
   ```batch
   setup-and-build.bat
   ```

This script will:
- Move source files to the correct directory structure
- Download and setup Gradle wrapper
- Setup the decompiled Minecraft workspace
- Build the mod

### Linux/Mac

1. Open Terminal in the project directory
2. Make the script executable and run it:
   ```bash
   chmod +x setup-and-build.sh
   ./setup-and-build.sh
   ```

## Manual Build Steps

If you prefer to build manually or the automated script fails:

### Step 1: Organize Source Files

The source files need to be in `src/main/kotlin/dev/wizard/meta/`

**Windows:**
```batch
mkdir src\main\kotlin\dev\wizard\meta
move *.kt src\main\kotlin\dev\wizard\meta\
move command src\main\kotlin\dev\wizard\meta\
move event src\main\kotlin\dev\wizard\meta\
move graphics src\main\kotlin\dev\wizard\meta\
move gui src\main\kotlin\dev\wizard\meta\
move manager src\main\kotlin\dev\wizard\meta\
move mixins src\main\kotlin\dev\wizard\meta\
move module src\main\kotlin\dev\wizard\meta\
move setting src\main\kotlin\dev\wizard\meta\
move structs src\main\kotlin\dev\wizard\meta\
move translation src\main\kotlin\dev\wizard\meta\
move util src\main\kotlin\dev\wizard\meta\
```

**Linux/Mac:**
```bash
mkdir -p src/main/kotlin/dev/wizard/meta
mv *.kt src/main/kotlin/dev/wizard/meta/
mv command event graphics gui manager mixins module setting structs translation util src/main/kotlin/dev/wizard/meta/
```

### Step 2: Setup Workspace

**Windows:**
```batch
gradlew.bat setupDecompWorkspace
```

**Linux/Mac:**
```bash
chmod +x gradlew
./gradlew setupDecompWorkspace
```

This step downloads Minecraft, Forge, and decompiles the source code. It may take 10-30 minutes on first run.

### Step 3: Build the Mod

**Windows:**
```batch
gradlew.bat build
```

**Linux/Mac:**
```bash
./gradlew build
```

### Step 4: Locate the Built JAR

The compiled mod will be at:
```
build/libs/meta-0.3B-10mq29.jar
```

## Development

### Running in Development Mode

**Windows:**
```batch
gradlew.bat runClient
```

**Linux/Mac:**
```bash
./gradlew runClient
```

This launches Minecraft with your mod loaded for testing.

### Setting up IDE

**IntelliJ IDEA (Recommended):**
```batch
gradlew.bat idea
```

Then open the generated `.ipr` file in IntelliJ IDEA.

**Eclipse:**
```batch
gradlew.bat eclipse
```

Then import the project in Eclipse.

## Troubleshooting

### "JAVA_HOME is not set"

Set the JAVA_HOME environment variable to your JDK 8 installation path.

**Windows:**
```batch
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
```

**Linux/Mac:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
```

### "Could not resolve dependencies"

Check your internet connection and try again. Gradle needs to download dependencies.

### "Task failed with an exception"

1. Clean the build: `gradlew.bat clean` (or `./gradlew clean`)
2. Delete the `.gradle` folder
3. Run setup again: `gradlew.bat setupDecompWorkspace`

### Build is very slow

First-time builds are slow because Gradle downloads and decompiles Minecraft. Subsequent builds are much faster.

### Out of memory errors

Increase Gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4G
```

## Additional Gradle Tasks

- `gradlew.bat clean` - Clean build artifacts
- `gradlew.bat build` - Build the mod
- `gradlew.bat runClient` - Run Minecraft client with mod
- `gradlew.bat setupDecompWorkspace` - Setup workspace
- `gradlew.bat idea` - Generate IntelliJ IDEA project files
- `gradlew.bat eclipse` - Generate Eclipse project files

## Project Structure

```
meta/
├── src/main/
│   ├── kotlin/dev/wizard/meta/  # Kotlin source files
│   │   ├── command/             # Command system
│   │   ├── event/               # Event system
│   │   ├── module/              # Modules (including AutomaticallyRegear)
│   │   ├── util/                # Utilities
│   │   └── ...
│   └── resources/               # Resources
│       ├── mcmod.info          # Mod metadata
│       └── mixins.*.json       # Mixin configurations
├── build.gradle                 # Build configuration
├── gradle.properties            # Gradle settings
├── settings.gradle              # Project settings
└── gradlew.bat / gradlew       # Gradle wrapper scripts
```

## Notes

- The mod is built for Minecraft 1.12.2 with Forge
- Requires Java 8 (will not work with newer Java versions)
- First build takes 10-30 minutes
- Subsequent builds take 1-5 minutes
