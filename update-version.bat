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
    echo Example: %0 2.10.5
    echo.
    pause
    exit /b 1
)

set NEW_VERSION=%~1

REM Get current version from pom.xml
for /f "tokens=2 delims=<>" %%a in ('findstr "<version>" pom.xml ^| findstr /v "parent" ^| findstr /v "maven"') do (
    set CURRENT_VERSION=%%a
    goto :version_found
)
:version_found

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

echo 🔄 Updating version in pom.xml...
powershell -Command "(Get-Content pom.xml) -replace '<version>%CURRENT_VERSION%</version>', '<version>%NEW_VERSION%</version>' | Set-Content pom.xml"
echo ✅ Updated pom.xml

echo 🔄 Updating version in README.md...
powershell -Command "(Get-Content README.md) -replace 'px-submission-tool-%CURRENT_VERSION%', 'px-submission-tool-%NEW_VERSION%' | Set-Content README.md"
echo ✅ Updated README.md

echo 🔄 Updating version in assembly.xml...
powershell -Command "(Get-Content assembly.xml) -replace 'px-submission-tool-\${project.version}', 'px-submission-tool-%NEW_VERSION%' | Set-Content assembly.xml"
echo ✅ Updated assembly.xml

echo 🔄 Updating version in launcher scripts...
powershell -Command "(Get-Content start.sh) -replace 'px-submission-tool-%CURRENT_VERSION%', 'px-submission-tool-%NEW_VERSION%' | Set-Content start.sh"
powershell -Command "(Get-Content start.bat) -replace 'px-submission-tool-%CURRENT_VERSION%', 'px-submission-tool-%NEW_VERSION%' | Set-Content start.bat"
echo ✅ Updated launcher scripts

echo.
echo 🎉 Version update completed successfully!
echo.
echo 📋 Summary of changes:
echo   • pom.xml: %CURRENT_VERSION% → %NEW_VERSION%
echo   • README.md: Updated all references
echo   • assembly.xml: Updated JAR references
echo   • start.sh: Updated JAR filename
echo   • start.bat: Updated JAR filename
echo.
echo 💡 Next steps:
echo   1. Review the changes: git diff
echo   2. Commit the changes: git commit -am "Update version to %NEW_VERSION%"
echo   3. Build the distribution: mvn clean package assembly:single
echo   4. Test the new distribution
echo.
pause
