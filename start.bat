@echo off
echo 🚀 PX Submission Tool - Windows Launcher
echo ======================================
echo.

REM Get the directory where this script is located
cd /d "%~dp0"
echo 📁 Working directory: %CD%
echo.

REM Check if JAR file exists
set JAR_FILE=px-submission-tool-2.10.4.jar
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

    REM Simple approach: just check if java -version works and assume it's too old
    echo System Java found but version checking is complex
    echo Assuming system Java is too old (need 21+)
    goto :download_jre
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
echo ❌ Failed to download JRE
echo.
echo Please install Java 21 or later from: https://adoptium.net/
echo Or ensure you have internet connection to download JRE automatically.
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

%JAVA_CMD% -jar "%JAR_FILE%"

REM Application has exited
echo.
echo PX Submission Tool has exited.
pause
