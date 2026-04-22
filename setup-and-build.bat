@echo off
echo ========================================
echo Meta Mod - Setup and Build Script
echo ========================================
echo.

echo Step 1: Moving source files to proper directory...
echo.

if not exist "src\main\kotlin\dev\wizard\meta" (
    mkdir "src\main\kotlin\dev\wizard\meta"
)

echo Moving Kotlin files...
for %%f in (*.kt) do (
    if exist "%%f" (
        move /Y "%%f" "src\main\kotlin\dev\wizard\meta\" >nul 2>&1
        echo   Moved %%f
    )
)

echo Moving directories...
for %%d in (command event graphics gui manager mixins module setting structs translation util) do (
    if exist "%%d" (
        if exist "src\main\kotlin\dev\wizard\meta\%%d" (
            rmdir /S /Q "src\main\kotlin\dev\wizard\meta\%%d"
        )
        move /Y "%%d" "src\main\kotlin\dev\wizard\meta\" >nul 2>&1
        echo   Moved %%d\
    )
)

echo.
echo Step 2: Setting up Gradle wrapper...
echo.

if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found!
    pause
    exit /b 1
)

echo Step 3: Setting up decompiled workspace...
echo This may take several minutes on first run...
echo.

call gradlew.bat setupDecompWorkspace

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Setup failed!
    pause
    exit /b 1
)

echo.
echo Step 4: Building the mod...
echo.

call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo The mod JAR file is located at:
echo build\libs\meta-0.3B-10mq29.jar
echo.
echo To run the mod in development:
echo   gradlew.bat runClient
echo.
pause
