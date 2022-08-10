PX-submission tool 
===================

PX Submission Tool is a desktop application to submit the data to proteomeXchange

# Quick Download 

[<img src="https://raw.githubusercontent.com/PRIDE-Toolsuite/pride-inspector/master/wiki/download.png">](http://ftp.pride.ebi.ac.uk/pub/databases/pride/resources/tools/submission-tool/latest/desktop/px-submission-tool.zip)

Please unzip and run `px-submission-tool-<version number>.jar` file!

# Requirements

* Java: Java JRE 1.8 (or above), which you can download [here](https://www.oracle.com/technetwork/java/javase/downloads/index.html)  
(Note: most computers should have Java installed already).

* Operating System: The current verison has been tested on Windows 10, Windows 7, Windows Vista, Linux and Max OS X, it should also work on other platforms. If you come across any problems on your platform, please contact the PRIDE Help Desk.

# Trouble shooting

For trouble shooting, please open your command-line and run following commands and check your environments are correctly setup.

1) `java -version` to check you have java installed. You should be able to see something like : java version "1.8.0_191". If you get an error message: `java -version is not recognized as an internal or external command ..`. This means, you have not setup your Java properly. If so, please install [Java](https://www.oracle.com/technetwork/java/javase/downloads/index.html) and restart the machine.
2) `echo $JAVA_HOME` to check you have setup environment variable properly.
3) `cd <path/to/the/px-submission-tool/directory>` Navigate to your submission tool folder
4) `java -jar px-submission-tool-<version number>.jar` to open the tool. Please replace the version number.
5) Open the px_submission.log file in px-submission-tool/log folder to see any errors or information.

If you need more help, please [contact us](mailto:pride-support@ebi.ac.uk) with the output if those commands and screenshots where appopriate. 
