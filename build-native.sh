#!/bin/bash
#
# Build native installer for current platform (macOS or Linux)
# Usage: ./build-native.sh [version]
#

set -e

echo "================================================"
echo "PX Submission Tool - Native Installer Builder"
echo "================================================"
echo

# Get version from argument or pom.xml
if [ -n "$1" ]; then
    VERSION="$1"
else
    VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
fi

# Clean version for installer (remove -SNAPSHOT)
CLEAN_VERSION=$(echo "$VERSION" | sed 's/-SNAPSHOT//' | sed 's/[^0-9.]//g')
if [ -z "$CLEAN_VERSION" ]; then
    CLEAN_VERSION="1.0.0"
fi

# Ensure version has at least 3 parts
IFS='.' read -ra PARTS <<< "$CLEAN_VERSION"
while [ ${#PARTS[@]} -lt 3 ]; do
    PARTS+=("0")
done
CLEAN_VERSION="${PARTS[0]}.${PARTS[1]}.${PARTS[2]}"

echo "Building version: $VERSION (installer: $CLEAN_VERSION)"
echo

# Detect platform
PLATFORM=""
if [[ "$OSTYPE" == "darwin"* ]]; then
    PLATFORM="macos"
    INSTALLER_TYPE="dmg"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    PLATFORM="linux"
    INSTALLER_TYPE="deb"
else
    echo "Error: Unsupported platform $OSTYPE"
    echo "This script supports macOS and Linux only."
    echo "For Windows, use build-native.bat"
    exit 1
fi

echo "Platform: $PLATFORM"
echo "Installer type: $INSTALLER_TYPE"
echo

# Check for JDK 21+
JAVA_VERSION=$(java -version 2>&1 | head -1 | sed -n 's/.*version "\([0-9]*\).*/\1/p')
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: JDK 21 or later required (found: $JAVA_VERSION)"
    echo "Install from: https://adoptium.net/"
    exit 1
fi
echo "Java version: $JAVA_VERSION"

# Check for jpackage
if ! command -v jpackage &> /dev/null; then
    echo "Error: jpackage not found"
    echo "Make sure you have JDK 21+ installed (not just JRE)"
    exit 1
fi
echo "jpackage: available"
echo

# Step 1: Build the fat JAR
echo "Step 1/3: Building fat JAR..."
mvn clean package -DskipTests -q

JAR_FILE=$(find target -name "px-submission-tool-*.jar" -not -name "*-sources*" -not -name "*-javadoc*" | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "Error: Could not find built JAR file"
    exit 1
fi
echo "JAR file: $JAR_FILE"
echo

# Step 2: Prepare jpackage input
echo "Step 2/3: Preparing jpackage input..."
rm -rf jpackage-input jpackage-output
mkdir -p jpackage-input jpackage-output

cp "$JAR_FILE" jpackage-input/
JAR_NAME=$(basename "$JAR_FILE")
echo "JAR name: $JAR_NAME"
echo

# Step 3: Build native installer
echo "Step 3/3: Building native installer..."

# Common jpackage options
JPACKAGE_OPTS=(
    --name "PX-Submission-Tool"
    --app-version "$CLEAN_VERSION"
    --vendor "PRIDE Team - EMBL-EBI"
    --description "ProteomeXchange Submission Tool for submitting proteomics data to PRIDE Archive"
    --input jpackage-input
    --main-jar "$JAR_NAME"
    --main-class uk.ac.ebi.pride.pxsubmit.Launcher
    --java-options "--enable-native-access=ALL-UNNAMED"
    --dest jpackage-output
)

if [ "$PLATFORM" = "macos" ]; then
    # Build DMG
    echo "Building DMG..."
    jpackage "${JPACKAGE_OPTS[@]}" \
        --type dmg \
        --mac-package-name "PX Submission Tool"

    # Also build PKG
    echo "Building PKG..."
    jpackage "${JPACKAGE_OPTS[@]}" \
        --type pkg \
        --mac-package-name "PX Submission Tool"
else
    # Linux - try DEB first
    if command -v dpkg &> /dev/null; then
        echo "Building DEB..."
        jpackage "${JPACKAGE_OPTS[@]}" \
            --type deb \
            --linux-menu-group "Science" \
            --linux-shortcut
    fi

    # Try RPM if available
    if command -v rpmbuild &> /dev/null; then
        echo "Building RPM..."
        jpackage "${JPACKAGE_OPTS[@]}" \
            --type rpm \
            --linux-menu-group "Science" \
            --linux-shortcut
    fi
fi

echo
echo "================================================"
echo "Build complete!"
echo "================================================"
echo
echo "Native installers created in: jpackage-output/"
ls -la jpackage-output/
echo
echo "To install:"
if [ "$PLATFORM" = "macos" ]; then
    echo "  DMG: Double-click and drag to Applications"
    echo "  PKG: Double-click to run installer"
else
    echo "  DEB: sudo dpkg -i jpackage-output/*.deb"
    echo "  RPM: sudo rpm -i jpackage-output/*.rpm"
fi
