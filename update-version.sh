#!/bin/bash

# PX Submission Tool - Version Update Script
# This script automatically updates version numbers in all relevant files

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 PX Submission Tool - Version Update Script${NC}"
echo "=============================================="
echo

# Check if version is provided
if [ $# -eq 0 ]; then
    echo -e "${RED}❌ Error: No version provided${NC}"
    echo
    echo "Usage: $0 <new-version>"
    echo "Example: $0 2.10.5"
    echo
    exit 1
fi

NEW_VERSION="$1"
CURRENT_VERSION=$(grep -o '<version>[^<]*</version>' pom.xml | head -1 | sed 's/<version>//;s/<\/version>//')

echo -e "${YELLOW}📋 Current version: ${CURRENT_VERSION}${NC}"
echo -e "${YELLOW}📋 New version: ${NEW_VERSION}${NC}"
echo

# Confirm the update
read -p "Do you want to update the version from ${CURRENT_VERSION} to ${NEW_VERSION}? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}⚠️ Version update cancelled${NC}"
    exit 0
fi

echo -e "${BLUE}🔄 Updating version in pom.xml...${NC}"
# Update pom.xml
sed -i.bak "s/<version>${CURRENT_VERSION}<\/version>/<version>${NEW_VERSION}<\/version>/g" pom.xml
rm pom.xml.bak

echo -e "${GREEN}✅ Updated pom.xml${NC}"

echo -e "${BLUE}🔄 Updating version in README.md...${NC}"
# Update README.md
sed -i.bak "s/px-submission-tool-${CURRENT_VERSION}/px-submission-tool-${NEW_VERSION}/g" README.md
rm README.md.bak

echo -e "${GREEN}✅ Updated README.md${NC}"

echo -e "${BLUE}🔄 Updating version in assembly.xml...${NC}"
# Update assembly.xml
sed -i.bak "s/px-submission-tool-\${project.version}/px-submission-tool-${NEW_VERSION}/g" assembly.xml
rm assembly.xml.bak

echo -e "${GREEN}✅ Updated assembly.xml${NC}"

echo -e "${BLUE}🔄 Updating version in launcher scripts...${NC}"
# Update start.sh
sed -i.bak "s/px-submission-tool-${CURRENT_VERSION}/px-submission-tool-${NEW_VERSION}/g" start.sh
rm start.sh.bak

# Update start.bat
sed -i.bak "s/px-submission-tool-${CURRENT_VERSION}/px-submission-tool-${NEW_VERSION}/g" start.bat
rm start.bat.bak

echo -e "${GREEN}✅ Updated launcher scripts${NC}"

echo
echo -e "${GREEN}🎉 Version update completed successfully!${NC}"
echo
echo -e "${BLUE}📋 Summary of changes:${NC}"
echo -e "  • pom.xml: ${CURRENT_VERSION} → ${NEW_VERSION}"
echo -e "  • README.md: Updated all references"
echo -e "  • assembly.xml: Updated JAR references"
echo -e "  • start.sh: Updated JAR filename"
echo -e "  • start.bat: Updated JAR filename"
echo
echo -e "${YELLOW}💡 Next steps:${NC}"
echo -e "  1. Review the changes: git diff"
echo -e "  2. Commit the changes: git commit -am \"Update version to ${NEW_VERSION}\""
echo -e "  3. Build the distribution: mvn clean package assembly:single"
echo -e "  4. Test the new distribution"
echo
