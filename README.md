PX-submission tool 
===================

PX Submission Tool is a desktop application to submit the data to proteomeXchange

# Quick Download 

[<img src="https://raw.githubusercontent.com/PRIDE-Toolsuite/pride-inspector/master/wiki/download.png">](http://ftp.pride.ebi.ac.uk/pub/databases/pride/resources/tools/submission-tool/latest/desktop/px-submission-tool.zip)

Please unzip and run `px-submission-tool-<version number>.jar` file!

# Requirements

* Java: Java SE 21 (or above) is required to run this application. You can download it from:
  * [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) (Recommended)
  * [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21)

## Java Installation Instructions

### macOS
1. Download the appropriate JDK installer for your Mac:
   * For Apple Silicon (M1/M2) Macs: Download the ARM64 DMG installer
   * For Intel Macs: Download the x64 DMG installer
2. Double-click the downloaded .dmg file
3. Follow the installation wizard instructions
4. Verify installation by opening Terminal and running:
   ```bash
   java -version
   ```
   You should see output indicating Java version 21.x.x

### Windows
1. Download the Windows x64 Installer (.exe) from the Oracle website
2. Run the downloaded installer
3. Follow the installation wizard instructions
4. Verify installation by opening Command Prompt and running:
   ```cmd
   java -version
   ```
   You should see output indicating Java version 21.x.x

### Linux
1. Download the appropriate package for your Linux distribution:
   * For Debian/Ubuntu: Download the .deb package
   * For Red Hat/Fedora: Download the .rpm package
   * For other distributions: Download the compressed archive (.tar.gz)
2. Install using your package manager or extract the archive
3. Verify installation by running:
   ```bash
   java -version
   ```
   You should see output indicating Java version 21.x.x

* Operating System: The current version has been tested on Windows 10, Windows 7, Windows Vista, Linux and Mac OS X, it should also work on other platforms. If you come across any problems on your platform, please contact the PRIDE Help Desk.

# Trouble shooting

For trouble shooting, please open your command-line and run following commands and check your environments are correctly setup.

1) `java -version` to check you have java installed. You should be able to see something like : java version "21.0". If you get an error message: `java -version is not recognized as an internal or external command ..`. This means, you have not setup your Java properly. If so, please install [Java](https://www.oracle.com/java/technologies/downloads/#java21) and restart the machine.
2) `echo $JAVA_HOME` to check you have setup environment variable properly.
3) `cd <path/to/the/px-submission-tool/directory>` Navigate to your submission tool folder
4) `java -jar px-submission-tool-<version number>.jar` to open the tool. Please replace the version number.
5) Open the px_submission.log file in px-submission-tool/log folder to see any errors or information.

If you need more help, please [contact us](mailto:pride-support@ebi.ac.uk) with the output if those commands and screenshots where appopriate. 

Java 21 or higher is required to run this application.

Please download and install Java 21 from:
https://www.oracle.com/java/technologies/downloads/#java21

After installation, make sure to use the new Java version to run this application.
