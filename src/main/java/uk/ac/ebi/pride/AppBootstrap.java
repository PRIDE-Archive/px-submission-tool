package uk.ac.ebi.pride;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.utilities.util.IOUtilities;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class AppBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(AppBootstrap.class);

    public static void main(String[] args) {
        new AppBootstrap().go();
    }

    /**
     * Method to run the PX Submission tool.
     */
    private void go() {
        // Log system information
        logSystemInformation();
        
        StringBuilder cmdBuffer = getCommand();
        callCommand(cmdBuffer);
    }

    private void callCommand(StringBuilder cmdBuffer) {
        String[] cmdArray = cmdBuffer.toString().split(" ");
        ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
        Process process;
        try {
            logger.info(cmdBuffer.toString());
            process = processBuilder.start();

            StreamProxy errorStreamProxy = new StreamProxy(process.getErrorStream(), System.err);
            StreamProxy outStreamProxy = new StreamProxy(process.getInputStream(), System.out);

            errorStreamProxy.start();
            outStreamProxy.start();

        } catch (IOException e) {
            logger.error("Error while bootstrapping the ProteomeXchange submission tool", e);
        }
    }

    private StringBuilder getCommand() {
        // read bootstrap properties
        Properties bootstrapProps = getBootstrapSettings();

        // createAttributedSequence the command
        StringBuilder cmdBuffer = new StringBuilder();
        cmdBuffer.append("java -cp ");
        if (isWindowsPlatform()) {
            cmdBuffer.append("\"");
        }

        cmdBuffer.append(System.getProperty("java.class.path"));
        if (isWindowsPlatform()) {
            cmdBuffer.append("\"");
        }

        // get proxy details
        String proxyHost = bootstrapProps.getProperty("px.proxy.host");
        String proxyPort = bootstrapProps.getProperty("px.proxy.port");

        if (proxyHost != null && proxyPort != null) {
            cmdBuffer.append(" -Dhttp.proxyHost=");
            cmdBuffer.append(proxyHost);
            cmdBuffer.append(" -Dhttp.proxyPort=");
            cmdBuffer.append(proxyPort);
        }

        // get upload protocol
        String uploadProtocol = bootstrapProps.getProperty("px.upload.protocol");

        String checkConnectivityToAsperaAndFtp = bootstrapProps.getProperty("px.upload.check.connection");

        logger.info("Testing connection " + checkConnectivityToAsperaAndFtp);

        if (checkConnectivityToAsperaAndFtp.equals("true")) {
            String asperaServer = bootstrapProps.getProperty("px.aspera.server.address");
            int asperaServerPort = Integer.parseInt(bootstrapProps.getProperty("px.aspera.server.port"));

            String ftpServer = bootstrapProps.getProperty("px.ftp.server.address");
            int ftpServerPort = Integer.parseInt(bootstrapProps.getProperty("px.ftp.server.port"));

            int ftpCheckExitStatus = checkFtpConnectivity(ftpServer, ftpServerPort);
            int asperaCheckExitStatus = checkAsperaConnectivity(asperaServer, asperaServerPort);

            if (ftpCheckExitStatus != 0 && asperaCheckExitStatus != 0) {
                logger.error("Both ftp and aspera not reachable");
                throw new RuntimeException("Both ftp and aspera not reachable please check network connection or with your system admin" +
                        "for opening firewall for below servers and port \n" +
                        "px.ftp.server.address = ftp-pride-private.ebi.ac.uk\n" +
                        "px.ftp.server.port = 21\n" +
                        "px.aspera.server.address = hx-fasp-1.ebi.ac.uk\n" +
                        "px.aspera.server.port = 33001");
            }
        }

        if (uploadProtocol != null) {
            cmdBuffer.append(" -Dpx.upload.protocol=");
            cmdBuffer.append(uploadProtocol);
        }

        // get number of threads encrypting
        String numberOfThreadsEncrypting = bootstrapProps.getProperty("px.checksum.threads.size");
        if (numberOfThreadsEncrypting != null) {
            cmdBuffer.append(" -Dpx.checksum.threads.size=");
            cmdBuffer.append(numberOfThreadsEncrypting);
        }

        // Get training setting
        String trainingModeStatus = bootstrapProps.getProperty("training.mode.status");
        if (trainingModeStatus != null) {
            if (trainingModeStatus.equals(AppContext.TRAINING_MODE_STATUS_ON)) {
                cmdBuffer.append(" -Dtraining.mode.status=");
                cmdBuffer.append(trainingModeStatus);
            }
        }

        cmdBuffer.append(" ");
        cmdBuffer.append(App.class.getName());
        return cmdBuffer;
    }

    private int checkAsperaConnectivity(String server, int port) {
        int exitStatus = 1;
        int timeout = 10000;
        Socket s = null;
        String reason = null;
        try {
            s = new Socket();
            s.setReuseAddress(true);
            SocketAddress sa = new InetSocketAddress(server, port);
            s.connect(sa, timeout);
        } catch (Exception e) {
            reason = getReason(server, port, null, e);
        } finally {
            if (s != null) {
                if (s.isConnected()) {
                    logger.info("Port " + port + " on " + server + " is reachable!");
                    exitStatus = 0;
                } else {
                    logger.error("Port " + port + " on " + server + " is not reachable; reason: " + reason);
                }
                try {
                    s.close();
                } catch (IOException e) {
                }
            }
        }
        return exitStatus;
    }

    private int checkFtpConnectivity(String server, int port) {
        int exitStatus = 1;
        int timeout = 10000;
        FTPClient ftpClient = new FTPClient();
        String reason = null;
        try {
            ftpClient.setConnectTimeout(timeout);
            ftpClient.connect(server, port);
        } catch (Exception e) {
            reason = getReason(server, port, null, e);
        } finally {
            if (ftpClient != null) {
                if (ftpClient.getReplyCode() == 220) {
                    logger.info("Port " + port + " on " + server + " is reachable!");
                    exitStatus = 0;
                } else {
                    logger.error("Port " + port + " on " + server + " is not reachable; reason: " + reason);
                }
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                }
            }
        }
        return exitStatus;
    }

    private String getReason(String server, int port, String reason, Exception e) {
        if (e.getMessage().contains("Connection refused") || e.getMessage().contains("Connection reset")) {
            reason = "port " + port + " on " + server + " is not reachable/blocked.";
        } else if (e instanceof UnknownHostException) {
            reason = "node " + server + " is unresolved.";
        } else if (e instanceof SocketTimeoutException) {
            reason = "timeout while attempting to reach node " + server + " on port " + port;
        }
        return reason;
    }

    /**
     * Log system information including tool version and operating system
     */
    private void logSystemInformation() {
        try {
            // Get application version from properties
            String toolVersion = getApplicationVersion();
            
            // Operating system information
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String osArch = System.getProperty("os.arch");
            
            // User and working directory
            String userHome = System.getProperty("user.home");
            String userDir = System.getProperty("user.dir");
            
            logger.info("=== PX Submission Tool System Information ===");
            logger.info("Tool Version: {}", toolVersion);
            logger.info("Operating System: {} {} ({})", osName, osVersion, osArch);
            logger.info("User Home: {}", userHome);
            logger.info("Working Directory: {}", userDir);
            logger.info("=============================================");
            
        } catch (Exception e) {
            logger.warn("Failed to log system information: {}", e.getMessage());
        }
    }
    
    /**
     * Get the application version from properties or manifest
     */
    private String getApplicationVersion() {
        try {
            // Try to get version from system properties first
            String version = System.getProperty("px.submission.tool.version");
            if (version != null && !version.isEmpty()) {
                return version;
            }
            
            // Try to get from package info
            Package pkg = AppBootstrap.class.getPackage();
            if (pkg != null && pkg.getImplementationVersion() != null) {
                return pkg.getImplementationVersion();
            }
            
            // Fallback to reading from properties file
            Properties props = getBootstrapSettings();
            String propVersion = props.getProperty("px.submission.tool.version");
            if (propVersion != null && !propVersion.isEmpty()) {
                return propVersion;
            }
            
            // Final fallback
            return "2.10.5";
            
        } catch (Exception e) {
            logger.warn("Failed to determine application version: {}", e.getMessage());
            return "2.10.5";
        }
    }
    


    /**
     * Check whether it is Windows platform
     *
     * @return boolean  true means it is running on windows
     */
    private boolean isWindowsPlatform() {
        String osName = System.getProperty("os.name");
        return osName.startsWith("Windows");
    }


    /**
     * Read bootstrap settings from config/config.props file.
     *
     * @return Properties   bootstrap settings.
     */
    public static Properties getBootstrapSettings() {
        // load properties
        Properties props = new Properties();

        InputStream inputStream = null;
        try {
            URL pathURL = IOUtilities.getFullPath(AppBootstrap.class, "config/config.props");
            File file = IOUtilities.convertURLToFile(pathURL);
            // input stream of the property file
            inputStream = new FileInputStream(file);
            props.load(inputStream);
        } catch (IOException e) {
            logger.error("Failed to load config/config.props file", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("Failed to close InputStream while reading config.props file", e);
                }
            }
        }

        return props;
    }

    /**
     * StreamProxy redirect the output stream and error stream to screen.
     */
    private static class StreamProxy extends Thread {
        final InputStream is;
        final PrintStream os;

        StreamProxy(InputStream is, PrintStream os) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    os.println(line);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }
}
