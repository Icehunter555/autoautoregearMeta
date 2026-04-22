@echo off
echo ========================================
echo Meta Mod - Quick Build Script
echo ========================================
echo.

echo Building the mod...
echo.

call gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed!
    echo.
    echo If this is your first build, run setup-and-build.bat instead
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
pause
