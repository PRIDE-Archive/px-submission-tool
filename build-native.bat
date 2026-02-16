@echo off
REM
REM Build native installer for Windows
REM Usage: build-native.bat [version]
REM

echo ================================================
echo PX Submission Tool - Native Installer Builder
echo ================================================
echo.

REM Get version from argument or use default
set VERSION=%1
if "%VERSION%"=="" (
    echo Extracting version from pom.xml...
    for /f "tokens=*" %%i in ('mvn help:evaluate -Dexpression^=project.version -q -DforceStdout') do set VERSION=%%i
)

REM Clean version for installer (remove -SNAPSHOT, keep only numbers and dots)
set CLEAN_VERSION=%VERSION:-SNAPSHOT=%
echo Building version: %VERSION% (installer: %CLEAN_VERSION%)
echo.

REM Check for Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java not found
    echo Install JDK 21 from: https://adoptium.net/
    pause
    exit /b 1
)

REM Check for jpackage
jpackage --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: jpackage not found
    echo Make sure you have JDK 21+ installed (not just JRE)
    pause
    exit /b 1
)
echo jpackage: available
echo.

REM Step 1: Build the fat JAR
echo Step 1/3: Building fat JAR...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo Error: Maven build failed
    pause
    exit /b 1
)

REM Find JAR file
for %%f in (target\px-submission-tool-*.jar) do (
    set JAR_FILE=%%f
    goto :found_jar
)
echo Error: Could not find built JAR file
pause
exit /b 1

:found_jar
echo JAR file: %JAR_FILE%
echo.

REM Step 2: Prepare jpackage input
echo Step 2/3: Preparing jpackage input...
if exist jpackage-input rmdir /s /q jpackage-input
if exist jpackage-output rmdir /s /q jpackage-output
mkdir jpackage-input
mkdir jpackage-output

copy "%JAR_FILE%" jpackage-input\
for %%f in (jpackage-input\*.jar) do set JAR_NAME=%%~nxf
echo JAR name: %JAR_NAME%
echo.

REM Step 3: Build native installers
echo Step 3/3: Building native installers...

REM Build EXE installer
echo Building EXE...
jpackage ^
    --type exe ^
    --name "PX-Submission-Tool" ^
    --app-version %CLEAN_VERSION% ^
    --vendor "PRIDE Team - EMBL-EBI" ^
    --description "ProteomeXchange Submission Tool for submitting proteomics data to PRIDE Archive" ^
    --input jpackage-input ^
    --main-jar %JAR_NAME% ^
    --main-class uk.ac.ebi.pride.pxsubmit.Launcher ^
    --java-options "--enable-native-access=ALL-UNNAMED" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --dest jpackage-output

if %errorlevel% neq 0 (
    echo Warning: EXE build failed, trying MSI...
)

REM Build MSI installer
echo Building MSI...
jpackage ^
    --type msi ^
    --name "PX-Submission-Tool" ^
    --app-version %CLEAN_VERSION% ^
    --vendor "PRIDE Team - EMBL-EBI" ^
    --description "ProteomeXchange Submission Tool for submitting proteomics data to PRIDE Archive" ^
    --input jpackage-input ^
    --main-jar %JAR_NAME% ^
    --main-class uk.ac.ebi.pride.pxsubmit.Launcher ^
    --java-options "--enable-native-access=ALL-UNNAMED" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --dest jpackage-output

if %errorlevel% neq 0 (
    echo Warning: MSI build may have failed
)

echo.
echo ================================================
echo Build complete!
echo ================================================
echo.
echo Native installers created in: jpackage-output\
dir jpackage-output\
echo.
echo To install:
echo   EXE: Double-click to run installer wizard
echo   MSI: Double-click to install (or use msiexec)
echo.
pause
