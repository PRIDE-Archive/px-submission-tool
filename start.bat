@echo off
setlocal enabledelayedexpansion
echo 🚀 PX Submission Tool - Windows Launcher
echo ======================================
echo.

REM Get the directory where this script is located
cd /d "%~dp0"
echo 📁 Working directory: %CD%
echo.

REM Check if JAR file exists
set JAR_FILE=px-submission-tool-${project.version}.jar
if not exist "%JAR_FILE%" (
    echo ❌ Error: Could not find %JAR_FILE%
    echo Current directory: %CD%
    echo Files found:
    dir /b *.jar
    echo.
    echo Please make sure you extracted the zip file completely.
    echo The JAR file should be in the same folder as this script.
    echo.
    pause
    exit /b 1
)

echo ✅ JAR file found: %JAR_FILE%
echo.

REM Check if Java is available - Download JRE if needed
echo 🔍 Checking for Java...

REM Try to find existing JRE first
set JAVA_CMD=
if exist "jre-windows\bin\java.exe" (
    echo ✅ Found existing Windows JRE: jre-windows\bin\java.exe
    set JAVA_CMD=jre-windows\bin\java.exe
    goto :java_found
)

REM If no JRE found, check system Java version first
echo ⚠️ No JRE found, checking system Java version...

java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo 🔍 Checking system Java version...

    REM Extract major version number from java -version output
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VER_RAW=%%~v
    )
    REM Extract major version (before first dot)
    for /f "tokens=1 delims=." %%m in ("!JAVA_VER_RAW!") do (
        set JAVA_MAJOR=%%m
    )
    echo System Java version: !JAVA_MAJOR!

    if !JAVA_MAJOR! GEQ 21 (
        echo ✅ System Java !JAVA_MAJOR! is compatible (21+^)
        set JAVA_CMD=java
        goto :java_found
    ) else (
        echo ⚠️ System Java !JAVA_MAJOR! is too old (need 21+^)
        goto :download_jre
    )
) else (
    echo ⚠️ No system Java found, attempting to download JRE...
    goto :download_jre
)

:download_jre
echo 📥 Downloading Java 21 JRE for Windows...

REM Check if JRE is already downloaded and working
if exist "jre-windows\bin\java.exe" (
    "jre-windows\bin\java.exe" -version >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ JRE already downloaded and working: jre-windows\bin\java.exe
        set JAVA_CMD=jre-windows\bin\java.exe
        goto :java_found
    )
)

REM Check if PowerShell is available for download
powershell -Command "Get-Command Invoke-WebRequest" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⬇️ Downloading JRE using PowerShell...
    powershell -Command "& {Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%%2B7/OpenJDK21U-jre_x64_windows_hotspot_21.0.6_7.zip' -OutFile 'jre-windows.zip'}"
    if %errorlevel% equ 0 (
        echo ✅ Download completed
        echo 📦 Extracting JRE...
        powershell -Command "& {Expand-Archive -Path 'jre-windows.zip' -DestinationPath '.' -Force}"
        if %errorlevel% equ 0 (
            echo ✅ JRE extracted successfully
            del jre-windows.zip
            if exist "jdk-21.0.6+7-jre\bin\java.exe" (
                ren "jdk-21.0.6+7-jre" "jre-windows"
                echo ✅ JRE directory renamed to: jre-windows
                set JAVA_CMD=jre-windows\bin\java.exe
                echo ✅ JRE ready: %JAVA_CMD%
                goto :java_found
            )
        )
    )
)

REM Check if curl is available as fallback
curl --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ⬇️ Downloading JRE using curl...
    curl -L -o jre-windows.zip "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%%2B7/OpenJDK21U-jre_x64_windows_hotspot_21.0.6_7.zip"
    if %errorlevel% equ 0 (
        echo ✅ Download completed
        echo 📦 Extracting JRE...
        powershell -Command "& {Expand-Archive -Path 'jre-windows.zip' -DestinationPath '.' -Force}" 2>nul
        if %errorlevel% equ 0 (
            echo ✅ JRE extracted successfully
            del jre-windows.zip
            if exist "jdk-21.0.6+7-jre\bin\java.exe" (
                ren "jdk-21.0.6+7-jre" "jre-windows"
                echo ✅ JRE directory renamed to: jre-windows
                set JAVA_CMD=jre-windows\bin\java.exe
                echo ✅ JRE ready: %JAVA_CMD%
                goto :java_found
            )
        )
    )
)

REM If download failed
echo ❌ Failed to download JRE automatically
echo.
echo 🔧 Troubleshooting options:
echo.
echo 1. Check your internet connection and try again
echo 2. Install Java 21 manually from: https://adoptium.net/
echo    - Download: OpenJDK 21 (JRE or JDK)
echo    - Install and ensure 'java' command works
echo 3. Use existing Java if you have version 21 or later:
echo    - Set JAVA_HOME environment variable
echo    - Or run: java -jar px-submission-tool-${project.version}.jar
echo.
echo 📚 For more help, see: https://github.com/PRIDE-Archive/px-submission-tool/blob/main/README.md
echo.
pause
exit /b 1
:java_found

echo.

REM Skip JAR verification - proceed directly to launch
echo ✅ Ready to launch application
echo.

REM Launch the application
echo 🚀 Launching PX Submission Tool...
echo.

REM Launch the application (GUI mode with file logging)
%JAVA_CMD% -jar "%JAR_FILE%"

REM Application has exited
echo.
echo PX Submission Tool has exited.
pause
