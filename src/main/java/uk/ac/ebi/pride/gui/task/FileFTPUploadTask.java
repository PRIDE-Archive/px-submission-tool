package uk.ac.ebi.pride.gui.task;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.CopyStreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.FileUtil;
import uk.ac.ebi.pride.gui.task.ftp.UploadCancelMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadErrorMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadFileSuccessMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadProgressMessage;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;

/**
 * Task for uploading files with improved reliability and error handling
 */
public class FileFTPUploadTask extends TaskAdapter<Void, UploadMessage> implements CopyStreamListener {
    private static final Logger logger = LoggerFactory.getLogger(FileFTPUploadTask.class);

    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB buffer
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final int CONNECTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final int DATA_TIMEOUT_MS = 300000; // 5 minutes

    private final DataFile dataFile;
    private final UploadDetail ftpDetail;
    private FTPClient ftp;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int retryCount;
    private String md5Checksum;

    public FileFTPUploadTask(DataFile dataFile, UploadDetail ftpDetail) {
        this.dataFile = dataFile;
        this.ftpDetail = ftpDetail;
        this.retryCount = 0;
    }

    @Override
    protected Void doInBackground() throws Exception {
        ftp = new FTPClient();
        setupFTPClient();

        try {
            boolean success = uploadFileWithRetry();
            if (success) {
                logger.info("File upload completed successfully: {}", dataFile.getFileName());
                publish(new UploadFileSuccessMessage(this, dataFile));
            }
        } catch (Exception e) {
            handleUploadError(e);
        } finally {
            releaseResources();
        }
        return null;
    }

    private void setupFTPClient() {
        ftp.setBufferSize(BUFFER_SIZE);
        ftp.setControlKeepAliveTimeout(Duration.ofSeconds(60));
        ftp.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        ftp.setDefaultTimeout(DATA_TIMEOUT_MS);
    }

    private boolean uploadFileWithRetry() throws Exception {
        while (retryCount < MAX_RETRIES) {
            try {
                if (connectToFTP()) {
                    if (uploadFile()) {
                        return true;
                    }
                }
            } catch (IOException e) {
                logger.warn("Upload attempt {} failed for file: {}", retryCount + 1, dataFile.getFileName(), e);
                if (retryCount < MAX_RETRIES - 1) {
                    long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount);
                    logger.info("Retrying in {} ms...", delay);
                    Thread.sleep(delay);
                }
            }
            retryCount++;
        }
        throw new IOException("Failed to upload file after " + MAX_RETRIES + " attempts: " + dataFile.getFileName());
    }

    private boolean connectToFTP() throws IOException {
        logger.info("Connecting to FTP server: {}:{}", ftpDetail.getHost(), ftpDetail.getPort());

        ftp.connect(ftpDetail.getHost(), ftpDetail.getPort());
        int reply = ftp.getReplyCode();

        if (!FTPReply.isPositiveCompletion(reply)) {
            logger.error("Failed to connect to FTP server. Reply code: {}", reply);
            return false;
        }

        if (!ftp.login(ftpDetail.getDropBox().getUserName(), ftpDetail.getDropBox().getPassword())) {
            logger.error("Failed to login to FTP server");
            return false;
        }

        logConnectionDetails();

        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);

        File folder = new File(ftpDetail.getFolder());
        if (!ftp.changeWorkingDirectory(folder.getName())) {
            logger.error("Failed to change to directory: {}", folder.getName());
            return false;
        }

        return true;
    }

    private void logConnectionDetails() {
        try {
            logger.debug("FTP Connection Details:");
            logger.debug("Local address: {}:{}", ftp.getLocalAddress().getCanonicalHostName(), ftp.getLocalPort());
            logger.debug("Remote address: {}:{}", ftp.getRemoteAddress().getCanonicalHostName(), ftp.getRemotePort());
            logger.debug("Passive host: {}:{}", ftp.getPassiveHost(), ftp.getPassivePort());
        } catch (Exception e) {
            logger.warn("Could not log all connection details", e);
        }
    }

    private boolean uploadFile() throws Exception {
        String fileName = dataFile.getFileName();
        long fileSize = 0;

        if (dataFile.isFile()) {
            File file = dataFile.getFile();
            fileSize = file.length();
            md5Checksum = calculateMD5(file);
            logger.info("Starting upload of file: {} (Size: {} bytes, MD5: {})",
                    fileName, fileSize, md5Checksum);

            inputStream = new BufferedInputStream(new FileInputStream(file));
        } else if (dataFile.isUrl()) {
            URL url = dataFile.getUrl();
            logger.info("Starting upload from URL: {}", url);
            inputStream = new BufferedInputStream(url.openStream());
        }

        try {
            boolean success = ftp.storeFile(fileName, inputStream);
            if (!success) {
                logger.error("FTP store file failed for: {}", fileName);
                return false;
            }

            // Verify file size
            long uploadedSize = verifyFileSize(fileName);
            if (fileSize > 0 && uploadedSize != fileSize) {
                logger.error("File size mismatch. Expected: {}, Actual: {}", fileSize, uploadedSize);
                return false;
            }

            return true;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private long verifyFileSize(String fileName) throws IOException {
        if (ftp.sendCommand("SIZE", fileName) != FTPReply.FILE_STATUS) {
            throw new IOException("Could not get file size");
        }
        return Long.parseLong(ftp.getReplyString().split(" ")[1].trim());
    }

    private String calculateMD5(File file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("Could not calculate MD5 checksum", e);
            return null;
        }
    }

    private void handleUploadError(Exception e) {
        String errorMessage = String.format("Failed to upload file: %s after %d attempts",
                dataFile.getFileName(), retryCount);
        logger.error(errorMessage, e);

        if (e instanceof CopyStreamException) {
            CopyStreamException cse = (CopyStreamException) e;
            logger.error("Transfer failed at {} bytes", cse.getTotalBytesTransferred());
            if (cse.getIOException() != null) {
                logger.error("Underlying IO error", cse.getIOException());
            }
        }

        // Enhance error message with Globus alternative
        String enhancedMessage = errorMessage + 
            "\n\nThis could be due to network restrictions, firewall settings, or network instability." +
            "\n\nAlternative options:" +
            "\n1. Go back one step and select Aspera upload instead" +
            "\n2. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus" +
            "\n3. Contact your system administrator to enable FTP ports (TCP 21)";

        publish(new UploadErrorMessage(this, dataFile, enhancedMessage));
    }

    private void releaseResources() {
        logger.debug("Cleaning up resources");
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (ftp != null && ftp.isConnected()) {
                ftp.logout();
                ftp.disconnect();
            }
        } catch (IOException e) {
            logger.warn("Error while releasing resources", e);
        }
    }

    @Override
    protected void cancelled() {
        logger.info("Upload cancelled for file: {}", dataFile.getFileName());
        releaseResources();
        publish(new UploadCancelMessage(this, dataFile));
    }

    @Override
    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
        publish(new UploadProgressMessage(this, dataFile, streamSize, totalBytesTransferred));
    }

    @Override
    public void bytesTransferred(CopyStreamEvent event) {
        bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());
    }
}
