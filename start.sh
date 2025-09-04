#!/bin/bash

echo "🚀 PX Submission Tool - Linux/macOS Launcher"
echo "============================================="
echo

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "📁 Working directory: $(pwd)"
echo

# Remove quarantine attributes (macOS Gatekeeper workaround)
echo "🔓 Removing quarantine attributes..."
xattr -cr . 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ Quarantine attributes removed successfully"
else
    echo "⚠️ Could not remove quarantine attributes (may require manual approval)"
fi
echo

# Check if JAR file exists
JAR_FILE="px-submission-tool-${project.version}.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: Could not find $JAR_FILE"
    echo "Current directory: $(pwd)"
    echo "Files found:"
    ls -la *.jar 2>/dev/null || echo "No JAR files found"
    echo
    echo "Please make sure you extracted the zip file completely."
    echo "The JAR file should be in the same folder as this script."
    echo
    read -p "Press Enter to exit..."
    exit 1
fi

echo "✅ JAR file found: $JAR_FILE"
echo

# Check if Java is available - Download JRE if needed
echo "🔍 Checking for Java..."

# Detect platform
PLATFORM=""
if [[ "$OSTYPE" == "darwin"* ]]; then
    PLATFORM="macos"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    PLATFORM="linux"
else
    PLATFORM="unknown"
fi

echo "🖥️ Detected platform: $PLATFORM"

# Function to download JRE
download_jre() {
    local platform=$1
    echo "📥 Downloading Java 21 JRE for $platform..."
    
    case $platform in
        "macos")
            JRE_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%2B7/OpenJDK21U-jre_x64_mac_hotspot_21.0.6_7.tar.gz"
            JRE_FILE="jre-macos.tar.gz"
            JRE_DIR="jre-macos"
            JRE_BIN="jre-macos/Contents/Home/bin/java"
            ;;
        "linux")
            JRE_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%2B7/OpenJDK21U-jre_x64_linux_hotspot_21.0.6_7.tar.gz"
            JRE_FILE="jre-linux.tar.gz"
            JRE_DIR="jre-linux"
            JRE_BIN="jre-linux/bin/java"
            ;;
        *)
            echo "❌ Unsupported platform: $platform"
            return 1
            ;;
    esac
    
    # Check if JRE is already downloaded and working
    if [ -f "$JRE_BIN" ] && "$JRE_BIN" -version &> /dev/null; then
        echo "✅ JRE already downloaded and working: $JRE_BIN"
        return 0
    fi
    
    # Check if curl or wget is available
    if command -v curl &> /dev/null; then
        DOWNLOAD_CMD="curl -L -o"
    elif command -v wget &> /dev/null; then
        DOWNLOAD_CMD="wget -O"
    else
        echo "❌ Error: Neither curl nor wget found. Cannot download JRE."
        return 1
    fi
    
    echo "⬇️ Downloading from: $JRE_URL"
    if $DOWNLOAD_CMD "$JRE_FILE" "$JRE_URL"; then
        echo "✅ Download completed"
        echo "📦 Extracting JRE..."
        if tar -xzf "$JRE_FILE"; then
            echo "✅ JRE extracted successfully"
            rm "$JRE_FILE"  # Clean up
            
            # Rename the extracted directory to the expected name
            if [ "$platform" = "macos" ] && [ -d "jdk-21.0.6+7-jre" ]; then
                mv "jdk-21.0.6+7-jre" "$JRE_DIR"
                echo "✅ JRE directory renamed to: $JRE_DIR"
            elif [ "$platform" = "linux" ] && [ -d "jdk-21.0.6+7-jre" ]; then
                mv "jdk-21.0.6+7-jre" "$JRE_DIR"
                echo "✅ JRE directory renamed to: $JRE_DIR"
            fi
            
            return 0
        else
            echo "❌ Failed to extract JRE"
            rm -f "$JRE_FILE"
            return 1
        fi
    else
        echo "❌ Failed to download JRE"
        rm -f "$JRE_FILE"
        return 1
    fi
}

# Try to find existing JRE first
JAVA_CMD=""
if [ "$PLATFORM" = "macos" ] && [ -f "jre-macos/Contents/Home/bin/java" ]; then
    echo "✅ Found existing macOS JRE: jre-macos/Contents/Home/bin/java"
    JAVA_CMD="jre-macos/Contents/Home/bin/java"
elif [ "$PLATFORM" = "macos" ] && [ -f "jre-macos/Home/bin/java" ]; then
    echo "✅ Found existing macOS JRE: jre-macos/Home/bin/java"
    JAVA_CMD="jre-macos/Home/bin/java"
elif [ "$PLATFORM" = "linux" ] && [ -f "jre-linux/bin/java" ]; then
    echo "✅ Found existing Linux JRE: jre-linux/bin/java"
    JAVA_CMD="jre-linux/bin/java"
elif [ -f "jre/bin/java" ]; then
    echo "✅ Found existing JRE: jre/bin/java"
    JAVA_CMD="jre/bin/java"
elif [ -f "jre/Contents/Home/bin/java" ]; then
    echo "✅ Found existing JRE: jre/Contents/Home/bin/java"
    JAVA_CMD="jre/Contents/Home/bin/java"
elif [ -f "Contents/Home/bin/java" ]; then
    echo "✅ Found existing JRE: Contents/Home/bin/java"
    JAVA_CMD="Contents/Home/bin/java"
fi

# If no JRE found, check system Java version first
if [ -z "$JAVA_CMD" ]; then
    echo "⚠️ No JRE found, checking system Java version..."
    
    if command -v java &> /dev/null; then
        # Extract Java version number
        JAVA_VERSION=$(java -version 2>&1 | head -1 | sed -n 's/.*version "\([0-9]*\).*/\1/p')
        echo "🔍 System Java version: $JAVA_VERSION"
        
        if [ "$JAVA_VERSION" -ge 21 ] 2>/dev/null; then
            JAVA_CMD="java"
            echo "✅ System Java $JAVA_VERSION is compatible (21+)"
            java -version 2>&1 | head -1
        else
            echo "⚠️ System Java $JAVA_VERSION is too old (need 21+)"
            
            # Try to download JRE for supported platforms
            if [ "$PLATFORM" = "macos" ] || [ "$PLATFORM" = "linux" ]; then
                echo "📥 Downloading Java 21 JRE..."
                if download_jre "$PLATFORM"; then
                    # Set JAVA_CMD based on platform
                    if [ "$PLATFORM" = "macos" ]; then
                        JAVA_CMD="jre-macos/Contents/Home/bin/java"
                    else
                        JAVA_CMD="jre-linux/bin/java"
                    fi
                    echo "✅ JRE ready: $JAVA_CMD"
                else
                    echo "❌ Failed to download JRE automatically"
                    echo
                    echo "🔧 Troubleshooting options:"
                    echo
                    echo "1. Check your internet connection and try again"
                    echo "2. Install Java 21 manually from: https://adoptium.net/"
                    echo "   - Download: OpenJDK 21 (JRE or JDK)"
                    echo "   - Install and ensure 'java' command works"
                    echo "3. Use existing Java if you have version 21 or later:"
                    echo "   - Set JAVA_HOME environment variable"
                    echo "   - Or run: java -jar px-submission-tool-${project.version}.jar"
                    echo
                    echo "📚 For more help, see: https://github.com/PRIDE-Archive/px-submission-tool/blob/main/README.md"
                    echo
                    read -p "Press Enter to exit..."
                    exit 1
                fi
            else
                echo "❌ Unsupported platform for automatic JRE download"
                echo
                echo "Please install Java 21 or later from: https://adoptium.net/"
                echo
                read -p "Press Enter to exit..."
                exit 1
            fi
        fi
    else
        echo "⚠️ No system Java found, attempting to download JRE..."
        
        # Try to download JRE for supported platforms
        if [ "$PLATFORM" = "macos" ] || [ "$PLATFORM" = "linux" ]; then
            if download_jre "$PLATFORM"; then
                # Set JAVA_CMD based on platform
                if [ "$PLATFORM" = "macos" ]; then
                    JAVA_CMD="jre-macos/Contents/Home/bin/java"
                else
                    JAVA_CMD="jre-linux/bin/java"
                fi
                echo "✅ JRE ready: $JAVA_CMD"
            else
                echo "❌ Failed to download JRE automatically"
                echo
                echo "🔧 Troubleshooting options:"
                echo
                echo "1. Check your internet connection and try again"
                echo "2. Install Java 21 manually from: https://adoptium.net/"
                echo "   - Download: OpenJDK 21 (JRE or JDK)"
                echo "   - Install and ensure 'java' command works"
                echo "3. Use existing Java if you have version 21 or later:"
                echo "   - Set JAVA_HOME environment variable"
                echo "   - Or run: java -jar px-submission-tool-${project.version}.jar"
                echo
                echo "📚 For more help, see: https://github.com/PRIDE-Archive/px-submission-tool/blob/main/README.md"
                echo
                read -p "Press Enter to exit..."
                exit 1
            fi
        else
            echo "❌ Unsupported platform for automatic JRE download"
            echo
            echo "Please install Java 21 or later from: https://adoptium.net/"
            echo
            read -p "Press Enter to exit..."
            exit 1
        fi
    fi
fi

echo

# Skip JAR verification - proceed directly to launch
echo "✅ Ready to launch application"
echo

# Launch the application
echo "🚀 Launching PX Submission Tool..."
echo

$JAVA_CMD -jar "$JAR_FILE"

# Application has exited
echo
echo "PX Submission Tool has exited."
read -p "Press Enter to exit..."
