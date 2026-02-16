@echo off
echo 🚀 PX Submission Tool - Windows Launcher
echo ======================================
echo.

REM Get the directory where this script is located
cd /d "%~dp0"
echo 📁 Working directory: %CD%
echo.

REM Find JAR file (using wildcard to avoid version mismatch issues)
set JAR_FILE=
for %%f in (px-submission-tool-*.jar) do (
    if not "%%f"=="px-submission-tool-*.jar" (
        set JAR_FILE=%%f
        goto :jar_found
    )
)

echo X Error: Could not find px-submission-tool-*.jar
echo Current directory: %CD%
echo Files found:
dir /b *.jar 2>nul || echo No JAR files found
echo.
echo Please make sure you extracted the zip file completely.
echo The JAR file should be in the same folder as this script.
echo.
pause
exit /b 1

:jar_found

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
echo No bundled JRE found, checking system Java version...

java -version >nul 2>&1
if %errorlevel% equ 0 (
    echo Checking system Java version...

    REM Extract Java version number using PowerShell
    for /f "tokens=*" %%i in ('powershell -Command "(java -version 2>&1 | Select-String 'version').ToString() -replace '.*version \"(\d+).*','$1'"') do set JAVA_VERSION=%%i

    if defined JAVA_VERSION (
        if %JAVA_VERSION% GEQ 21 (
            echo System Java %JAVA_VERSION% is compatible (21+)
            set JAVA_CMD=java
            goto :java_found
        ) else (
            echo System Java %JAVA_VERSION% is too old (need 21+)
            goto :download_jre
        )
    ) else (
        echo Could not determine Java version, attempting to use system Java...
        set JAVA_CMD=java
        goto :java_found
    )
) else (
    echo No system Java found, attempting to download JRE...
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
echo    - Or run: java -jar %JAR_FILE%
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
REM --enable-native-access=ALL-UNNAMED suppresses JavaFX native library warnings
%JAVA_CMD% --enable-native-access=ALL-UNNAMED -jar "%JAR_FILE%"

REM Application has exited
echo.
echo PX Submission Tool has exited.
pause
