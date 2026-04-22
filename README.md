# Meta - Minecraft 1.12.2 Utility Mod

A Minecraft utility mod with various features including the AutomaticallyRegear module.

## Requirements

- Java 8 (JDK 1.8)
- Gradle 4.9 (included via wrapper)

## Building

### Windows

```bash
gradlew.bat setupDecompWorkspace
gradlew.bat build
```

### Linux/Mac

```bash
chmod +x gradlew
./gradlew setupDecompWorkspace
./gradlew build
```

The built JAR file will be located in `build/libs/meta-0.3B-10mq29.jar`

## Development

### Setup Development Environment

```bash
# Windows
gradlew.bat setupDecompWorkspace
gradlew.bat idea

# Linux/Mac
./gradlew setupDecompWorkspace
./gradlew idea
```

### Running in Development

```bash
# Windows
gradlew.bat runClient

# Linux/Mac
./gradlew runClient
```

## Project Structure

```
.
├── src/main/
│   ├── kotlin/dev/wizard/meta/  # Source files (currently in root, need to move)
│   └── resources/               # Resources (mcmod.info, mixins)
├── build.gradle                 # Build configuration
├── gradle.properties            # Gradle properties
└── settings.gradle              # Project settings
```

## Features

### AutomaticallyRegear Module

Located in `module/modules/beta/AutomaticallyRegear.kt`

- Automatically monitors kit inventory
- Triggers regear when items fall below threshold
- Configurable settings for threshold, delay, and keybind
- Integrates with existing Kit and AutoRegear systems

## Notes

**IMPORTANT**: The source files are currently in the root directory. They need to be moved to `src/main/kotlin/dev/wizard/meta/` for the build to work properly.

To move files:

```bash
# Windows PowerShell
Move-Item -Path "*.kt" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "command" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "event" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "graphics" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "gui" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "manager" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "mixins" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "module" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "setting" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "structs" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "translation" -Destination "src/main/kotlin/dev/wizard/meta/"
Move-Item -Path "util" -Destination "src/main/kotlin/dev/wizard/meta/"
```

After moving files, run the build commands above.
