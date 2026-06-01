@echo off
setlocal enabledelayedexpansion

REM PX Submission Tool - Version Update Script (Windows)
REM This script automatically updates version numbers in all relevant files

echo 🚀 PX Submission Tool - Version Update Script
echo ==============================================
echo.

REM Check if version is provided
if "%~1"=="" (
    echo ❌ Error: No version provided
    echo.
    echo Usage: %0 ^<new-version^>
    echo Example: %0 2.11.1
    echo.
    pause
    exit /b 1
)

set NEW_VERSION=%~1

REM Check if Maven is available
echo 🔍 Checking for Maven...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Error: Maven is not installed or not in PATH
    echo Please install Maven and ensure it's available in your PATH
    echo Download from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
echo ✅ Maven is available

REM Get current version from Maven
echo 🔍 Reading current version from Maven...
for /f "tokens=*" %%a in ('mvn help:evaluate -Dexpression=project.version -q -DforceStdout') do (
    set CURRENT_VERSION=%%a
    goto :version_found
)
:version_found

REM Check if version was read successfully
if "%CURRENT_VERSION%"=="" (
    echo ❌ Error: Could not read current version from Maven
    echo Please check that pom.xml is valid and Maven can read it
    pause
    exit /b 1
)

echo 📋 Current version: %CURRENT_VERSION%
echo 📋 New version: %NEW_VERSION%
echo.

REM Confirm the update
set /p CONFIRM="Do you want to update the version from %CURRENT_VERSION% to %NEW_VERSION%? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo ⚠️ Version update cancelled
    pause
    exit /b 0
)

echo 🔄 Updating version in pom.xml using Maven Versions Plugin...
mvn versions:set -DnewVersion=%NEW_VERSION% -DgenerateBackupPoms=false
if %errorlevel% neq 0 (
    echo ❌ Error: Failed to update version in pom.xml
    echo Please check that Maven is installed and pom.xml is valid
    pause
    exit /b 1
)
echo ✅ Updated pom.xml using Maven Versions Plugin

echo 🔄 Updating version in README.md...
powershell -Command "(Get-Content README.md) -replace 'px-submission-tool-%CURRENT_VERSION%\.jar', 'px-submission-tool-%NEW_VERSION%.jar' | Set-Content README.md"
if %errorlevel% neq 0 (
    echo ❌ Error: Failed to update README.md
    pause
    exit /b 1
)
echo ✅ Updated README.md

echo 🔄 Updating version in assembly.xml...
powershell -Command "(Get-Content assembly.xml) -replace 'px-submission-tool-\$\{project\.version\}', 'px-submission-tool-%NEW_VERSION%' | Set-Content assembly.xml"
if %errorlevel% neq 0 (
    echo ❌ Error: Failed to update assembly.xml
    pause
    exit /b 1
)
echo ✅ Updated assembly.xml

echo ℹ️ Launcher scripts use Maven filtering (${project.version}) - no manual update needed

echo.
echo 🎉 Version update completed successfully!
echo.
echo 📋 Summary of changes:
echo   • pom.xml: %CURRENT_VERSION% → %NEW_VERSION% (using Maven Versions Plugin)
echo   • README.md: Updated JAR filename references
echo   • assembly.xml: Updated JAR filename references
echo   • start.sh: Uses Maven filtering (${project.version})
echo   • start.bat: Uses Maven filtering (${project.version})
echo.
echo 💡 Next steps:
echo   1. Review the changes: git diff
echo   2. Commit the changes: git commit -am "Update version to %NEW_VERSION%"
echo   3. Build the distribution: mvn -Pdist clean package assembly:single
echo   4. Test the new distribution
echo.
pause
