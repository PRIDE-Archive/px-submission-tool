# PX Submission Tool

A JavaFX-based GUI application for submitting proteomics data to the PRIDE Archive and ProteomeXchange.

## Table of Contents

- [Quick Start (End Users)](#quick-start-end-users)
- [Developer Guide](#developer-guide)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Support](#support)

---

## Quick Start (End Users)

### Download & Extract
1. **Download**: Use one of these options:
   - **Latest version**: [px-submission-tool-latest.zip](https://github.com/PRIDE-Archive/px-submission-tool/releases/latest/download/px-submission-tool-latest.zip)
   - **Specific version**: Visit [GitHub Releases](https://github.com/PRIDE-Archive/px-submission-tool/releases)
2. **Extract**: Extract the zip file to a folder of your choice
3. **Launch**: Follow the OS-specific launch instructions below

### Smart JRE Management
- **Small Download**: The tool is only ~140MB (vs 400MB+ before)
- **Auto-Download**: Java 21 JRE is downloaded automatically if needed
- **Smart Detection**: Uses your system Java if it's version 21 or higher
- **No Re-downloading**: Once downloaded, JRE stays in the tool directory

### Windows Users
1. Extract the zip file completely
2. Double-click `start.bat`
3. First run will download Java 21 JRE if needed (~250MB)

### macOS Users
1. Extract the zip file completely
2. Run: `chmod +x start.sh && ./start.sh`
3. First run will download Java 21 JRE if needed (~250MB)

### Linux Users
1. Extract the zip file completely
2. Run: `chmod +x start.sh && ./start.sh`
3. First run will download Java 21 JRE if needed (~250MB)

---

## Developer Guide

### Prerequisites

- **Java 21** or higher (JDK, not JRE)
- **Maven 3.8+**
- **Git**

### Clone the Repository

```bash
git clone https://github.com/PRIDE-Archive/px-submission-tool.git
cd px-submission-tool
```

### Build the Project

```bash
# Compile only
mvn compile

# Build JAR with dependencies
mvn package

# Build and skip tests
mvn package -DskipTests

# Clean build
mvn clean package
```

### Run the Application

#### Option 1: Using Maven (Development)
```bash
mvn javafx:run
```

#### Option 2: Using the JAR (After Build)
```bash
java -jar target/px-submission-tool-<version>.jar
```

#### Option 3: With Debug Mode
```bash
java -jar target/px-submission-tool-<version>.jar --debug
```

### Command Line Options

| Option | Description |
|--------|-------------|
| `--debug`, `-d` | Enable debug mode with verbose logging |
| `--training`, `-t` | Enable training mode (no actual upload) |
| `--file`, `-f FILE` | Load submission from file |
| `--help`, `-h` | Show help message |

### IDE Setup

#### IntelliJ IDEA
1. Open the project as a Maven project
2. Set Project SDK to Java 21
3. Run `PxSubmitApplication` or `Launcher` as the main class

#### Eclipse
1. Import as Maven project
2. Configure Java 21 as the JRE
3. Run `uk.ac.ebi.pride.pxsubmit.Launcher` as Java Application

#### VS Code
1. Install Java Extension Pack
2. Open the folder
3. Run via Maven: `mvn javafx:run`

---

## Project Structure

```
px-submission-tool/
├── src/
│   └── main/
│       ├── java/uk/ac/ebi/pride/pxsubmit/
│       │   ├── Launcher.java              # Application entry point
│       │   ├── PxSubmitApplication.java   # JavaFX Application class
│       │   ├── config/                    # Configuration classes
│       │   │   └── AppConfig.java
│       │   ├── controller/                # Wizard step controllers
│       │   │   ├── AbstractWizardStep.java
│       │   │   ├── WizardController.java
│       │   │   ├── WizardStep.java
│       │   │   ├── WelcomeStep.java
│       │   │   ├── LoginStep.java
│       │   │   ├── SubmissionTypeStep.java
│       │   │   ├── FileSelectionStep.java
│       │   │   ├── FileReviewStep.java
│       │   │   ├── SampleMetadataStep.java
│       │   │   ├── ProjectMetadataStep.java
│       │   │   ├── SummaryStep.java
│       │   │   ├── ChecksumComputationStep.java
│       │   │   └── SubmissionStep.java
│       │   ├── model/                     # Data models
│       │   │   ├── FileEntry.java
│       │   │   └── SubmissionModel.java
│       │   ├── service/                   # Business logic services
│       │   │   ├── ApiService.java
│       │   │   ├── AuthService.java
│       │   │   ├── ChecksumService.java
│       │   │   ├── FtpUploadService.java
│       │   │   ├── AsperaUploadService.java
│       │   │   ├── OlsService.java
│       │   │   ├── SdrfParserService.java
│       │   │   ├── UploadManager.java
│       │   │   └── ValidationService.java
│       │   ├── util/                      # Utility classes
│       │   │   ├── DebugMode.java
│       │   │   └── FileTypeDetector.java
│       │   └── view/                      # UI components
│       │       ├── ThemeManager.java
│       │       ├── component/
│       │       │   ├── ChipInput.java
│       │       │   ├── FileClassificationPanel.java
│       │       │   ├── FileTableView.java
│       │       │   ├── NotificationPane.java
│       │       │   ├── OlsAutocomplete.java
│       │       │   ├── ToolDetectionPanel.java
│       │       │   └── ValidationFeedback.java
│       │       └── dialog/
│       │           ├── DialogHelper.java
│       │           └── SettingsDialog.java
│       └── resources/
│           ├── fxml/                      # JavaFX FXML layouts
│           ├── css/                       # Stylesheets
│           ├── icon/                      # Application icons
│           └── prop/                      # Configuration properties
├── aspera/                                # Aspera transfer binaries
├── config/                                # Runtime configuration
├── help/                                  # Help documentation
├── pom.xml                                # Maven build configuration
├── assembly.xml                           # Assembly descriptor
├── start.bat                              # Windows launcher
└── start.sh                               # Linux/macOS launcher
```

### Architecture Overview

The application follows an MVC-like pattern with a wizard-based workflow:

1. **Launcher** - Entry point that delegates to PxSubmitApplication
2. **PxSubmitApplication** - JavaFX Application that initializes the UI
3. **WizardController** - Manages navigation between steps
4. **WizardStep** - Interface for each step in the submission workflow
5. **SubmissionModel** - Central state management using JavaFX properties
6. **Services** - Handle API calls, file operations, and uploads

### Key Technologies

- **JavaFX 21** - Modern UI framework
- **FXML** - Declarative UI layouts
- **CSS** - Styling and theming
- **Maven** - Build and dependency management
- **Jackson** - JSON processing
- **Apache Commons** - File transfer utilities

---

## Troubleshooting

### Common Build Issues

#### "JavaFX runtime components are missing"
Ensure you're using JDK 21 and have the JavaFX dependencies in your classpath:
```bash
mvn clean compile
```

#### "Cannot find symbol" errors after checkout
```bash
mvn clean install -DskipTests
```

### Common Runtime Issues

#### "Java not found" or "JRE download failed"
1. Check Internet connection for first-time JRE download
2. Manual Install: Download Java 21 from [Adoptium](https://adoptium.net/)
3. Set `JAVA_HOME` environment variable

#### "Permission denied" (macOS/Linux)
```bash
chmod +x start.sh
```

#### "Security warning" (macOS)
```bash
xattr -cr .
```

#### Transfer failures (Aspera/FTP)
- Try the alternative transfer method suggested by the tool
- Use **Globus** as a reliable alternative: [https://www.ebi.ac.uk/pride/markdownpage/globus](https://www.ebi.ac.uk/pride/markdownpage/globus)

---

## System Requirements

### End Users
- **Operating System**: Windows 10+, macOS 10.14+, or Linux (64-bit)
- **Memory**: Minimum 4GB RAM, recommended 8GB+
- **Disk Space**: 500MB for installation + space for data files
- **Network**: Internet connection for data submission
- **Java**: Auto-downloaded if needed (Java 21)

### Developers
- **JDK**: Java 21 or higher
- **Maven**: 3.8 or higher
- **Memory**: 8GB+ RAM recommended
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

---

## Support

- **Documentation**: [PRIDE Archive](https://www.ebi.ac.uk/pride/)
- **Issues**: [GitHub Issues](https://github.com/PRIDE-Archive/px-submission-tool/issues)
- **Email**: pride-support@ebi.ac.uk
- **ProteomeXchange**: [ProteomeXchange](https://www.proteomexchange.org/)

---

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
