# PX Submission Tool

A Java-based GUI application for submitting proteomics data to the PRIDE Archive and ProteomeXchange.

## 🚀 Quick Start

### **Download & Extract**
1. **Download**: Use one of these options:
   - **Latest version**: [px-submission-tool-latest.zip](https://github.com/PRIDE-Archive/px-submission-tool/releases/latest/download/px-submission-tool-latest.zip)
   - **Specific version**: Visit [GitHub Releases](https://github.com/PRIDE-Archive/px-submission-tool/releases)
   - **Universal link**: Always points to the latest: `https://github.com/PRIDE-Archive/px-submission-tool/releases/latest/download/px-submission-tool-latest.zip`
2. **Extract**: Extract the zip file to a folder of your choice
3. **Launch**: Follow the OS-specific launch instructions below

### **📦 Smart JRE Management**
- **Small Download**: The tool is now only ~140MB (vs 400MB+ before)
- **Auto-Download**: Java 21 JRE is downloaded automatically if needed
- **Smart Detection**: Uses your system Java if it's version 21 or higher
- **No Re-downloading**: Once downloaded, JRE stays in the tool directory
- **Internet Required**: First run needs internet to download JRE (if system Java is too old)

---

## 🖥️ **Windows Users**
1. **Extract** the zip file completely
2. **Double-click** `start.bat`
3. **First Run**: If you don't have Java 21+, the tool will download it automatically (~250MB)
4. **Subsequent Runs**: Uses the downloaded JRE (no re-downloading)
5. **Done!** 🎉

### **Windows Issues?**
- **Quick Fix**: Make sure JAR file is in the same folder as start.bat
- **Manual Launch**: `java -jar px-submission-tool-2.10.4.jar`
- **No Internet**: Install Java 21+ manually from [Adoptium](https://adoptium.net/)
- **JRE Location**: Downloaded JRE is stored in `jre-windows/` folder

---

## 🍎 **macOS Users**
1. **Extract** the zip file completely
2. **Run**: `./start.sh`
3. **First Run**: If you don't have Java 21+, the tool will download it automatically (~250MB)
4. **Subsequent Runs**: Uses the downloaded JRE (no re-downloading)
5. **Done!** 🎉

### **macOS Issues?**
- **Permission Fix**: `chmod +x start.sh`
- **Manual Launch**: `java -jar px-submission-tool-2.10.4.jar`
- **No Internet**: Install Java 21+ manually from [Adoptium](https://adoptium.net/)
- **JRE Location**: Downloaded JRE is stored in `jre-macos/` folder



---

## 🐧 **Linux Users**
1. **Extract** the zip file completely
2. **Run**: `./start.sh`
3. **First Run**: If you don't have Java 21+, the tool will download it automatically (~250MB)
4. **Subsequent Runs**: Uses the downloaded JRE (no re-downloading)
5. **Done!** 🎉

### **Linux Issues?**
- **Permission Fix**: `chmod +x start.sh`
- **Manual Launch**: `java -jar px-submission-tool-2.10.4.jar`
- **No Internet**: Install Java 21+ manually from [Adoptium](https://adoptium.net/)
- **JRE Location**: Downloaded JRE is stored in `jre-linux/` folder

---

## 🔧 **How Smart JRE Management Works**

### **First Run (with internet):**
1. **Check System Java**: If you have Java 21+, use it
2. **Download JRE**: If system Java is too old, download Java 21 JRE (~250MB)
3. **Store Locally**: JRE is saved in platform-specific folder:
   - **Windows**: `jre-windows/`
   - **macOS**: `jre-macos/`
   - **Linux**: `jre-linux/`

### **Subsequent Runs:**
1. **Find Local JRE**: Automatically detects downloaded JRE
2. **Skip Download**: No re-downloading, instant startup
3. **Launch Application**: Uses the local JRE

### **Benefits:**
- ✅ **Smaller initial download**: ~140MB vs 400MB+
- ✅ **Faster subsequent launches**: No download time
- ✅ **Offline capable**: Works without internet after first run
- ✅ **Cross-platform**: Automatic platform detection

---

## ⚠️ **Troubleshooting**

### **Permission Denied Errors (Linux/macOS)**
If you get permission errors, make the scripts executable:
```bash
chmod +x start.sh
```

### **Java Not Found**
The tool includes a bundled JRE, but if you encounter issues:
- Ensure you have Java 8 or later installed
- Set `JAVA_HOME` environment variable
- Use the bundled JRE: `./jre-macos/Home/bin/java -jar px-submission-tool-2.10.4.jar` (macOS) or `./jre-linux/bin/java -jar px-submission-tool-2.10.4.jar` (Linux)

### **Transfer Method Failures**
If Aspera or FTP transfers fail:
- The tool will suggest using **Globus** as an alternative
- Visit: [https://www.ebi.ac.uk/pride/markdownpage/globus](https://www.ebi.ac.uk/pride/markdownpage/globus)
- Globus provides reliable, high-speed file transfers

### **macOS Security Warnings**
- Right-click the `.app` file and select "Open"
- Go to System Preferences → Security & Privacy → General
- Click "Allow Anyway" for the PX Submission Tool

---

## 📁 **File Structure**

```
px-submission-tool-2.10.4/
├── start.bat                        # Windows launcher
├── start.sh                         # Linux/macOS launcher
├── px-submission-tool-2.10.4.jar # Main application JAR
├── jre-windows/                     # Windows JRE
├── jre-linux/                       # Linux JRE
├── jre-macos/                       # macOS JRE
├── aspera/                          # Aspera transfer binaries
├── config/                          # Configuration files
└── help/                            # Help documentation
```

---

## 🔧 **Advanced Usage**

### **Custom Java Installation**
If you prefer to use your own Java installation:
```bash
# Set JAVA_HOME to your Java installation
export JAVA_HOME=/path/to/your/java
java -jar px-submission-tool-2.10.4.jar
```

### **Proxy Configuration**
The tool automatically detects system proxy settings. To configure manually:
```bash
# Set proxy environment variables
export http_proxy=http://proxy.example.com:8080
export https_proxy=http://proxy.example.com:8080
```

### **Logging**
All application logs are consolidated into a single file:
- **Location**: `logs/px-submission.log`
- **Contains**: Transfer progress, API calls, errors, and debugging information
- **Use**: Send this file to support if you encounter issues

---

## 📞 **Support**

### **Common Issues**
- **Transfer failures**: Use Globus alternative as suggested by the tool
- **Login issues**: Check your PRIDE credentials and network connectivity
- **Progress bar stuck**: Check the logs for detailed error information

### **Getting Help**
1. Check the logs in `logs/px-submission.log`
2. Try the alternative transfer methods suggested by the tool
3. Visit the [PRIDE documentation](https://www.ebi.ac.uk/pride/markdownpage/globus)
4. Contact PRIDE support with your log file

---

## 🔄 **Updates**

### **Automatic Update Checking**
- **Built-in checker**: The tool automatically checks for updates on startup
- **GitHub integration**: Uses GitHub Releases API for reliable update detection
- **Smart notifications**: Shows release notes and what's new in each version
- **Universal links**: Always provides the latest download link

### **Download Options**
- **Versioned releases**: `px-submission-tool-2.10.4.zip` (specific version)
- **Latest release**: `px-submission-tool-latest.zip` (always current)
- **Universal link**: `https://github.com/PRIDE-Archive/px-submission-tool/releases/latest/download/px-submission-tool-latest.zip`

### **Update Process**
1. **Tool notification**: You'll see an update dialog when a new version is available
2. **Review changes**: Read what's new in the release notes
3. **Download**: Click to open the GitHub release page
4. **Install**: Download and extract the new version
5. **Restart**: The tool may suggest restarting for major updates

### **Backup Recommendations**
- Always backup your submission data before updating
- Export any in-progress submissions
- Save your configuration files if customized

---

## 📋 **System Requirements**

- **Operating System**: Windows 10+, macOS 10.14+, or Linux (64-bit)
- **Memory**: Minimum 4GB RAM, recommended 8GB+
- **Disk Space**: 2GB for installation, additional space for data files
- **Network**: Internet connection for data submission
- **Java**: Bundled JRE included (Java 8+ compatible)

---

*For more information, visit the [PRIDE Archive](https://www.ebi.ac.uk/pride/) or [ProteomeXchange](https://www.proteomexchange.org/)*
