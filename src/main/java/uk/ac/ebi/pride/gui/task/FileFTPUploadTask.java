package uk.ac.ebi.pride.gui.task;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.FileUtil;
import uk.ac.ebi.pride.gui.task.ftp.*;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Task for uploading all the files
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileFTPUploadTask extends TaskAdapter<Void, UploadMessage> implements CopyStreamListener {
    public static final Logger logger = LoggerFactory.getLogger(FileFTPUploadTask.class);

    public static final int BUFFER_SIZE = 2048;

    private DataFile dataFile;
    private UploadDetail ftpDetail;
    private FTPClient ftp;
    private InputStream inputStream;
    private OutputStream outputStream;

    public FileFTPUploadTask(DataFile dataFile,
                             UploadDetail ftpDetail) {
        this.dataFile = dataFile;
        this.ftpDetail = ftpDetail;
    }

    @Override
    protected Void doInBackground() throws Exception {
        ftp = new FTPClient();
        ftp.setControlKeepAliveTimeout(Duration.ofSeconds(300));

        inputStream = null;
        outputStream = null;
        String fileName = dataFile.getFileName();
        try {
            int reply;
            ftp.connect(ftpDetail.getHost(), ftpDetail.getPort());
            ftp.login(ftpDetail.getDropBox().getUserName(), ftpDetail.getDropBox().getPassword());

            logger.debug("FTP local address: " + ftp.getLocalAddress().getCanonicalHostName() + ":" + ftp.getLocalPort());
            logger.debug("FTP passive host address: " + ftp.getPassiveHost() + ":" + ftp.getPassivePort());
            logger.debug("FTP remote host address: " + ftp.getRemoteAddress().getCanonicalHostName() + ":" + ftp.getRemotePort());

            // After connection attempt, you should check the reply code to verify
            // success.
            reply = ftp.getReplyCode();
            logger.debug("FTP connection reply code: " + reply);

            if (!FTPReply.isPositiveCompletion(reply)) {
                publish(new UploadErrorMessage(this, dataFile, "Failed to connect to FTP server"));
                return null;
            }

            // passive mode
            ftp.enterLocalPassiveMode();
            logger.debug("FTP enter local passive mode");

            // change working directory
            File folder = new File(ftpDetail.getFolder());
            ftp.changeWorkingDirectory(folder.getName());
            logger.info("Changed to the correct FTP directory for upload: " + ftpDetail.getFolder());

            if (dataFile.isFile()) {
                // check whether the file is binary file
                File fileToUpload = dataFile.getFile();
                logger.debug("About to upload file: " + fileToUpload.getAbsolutePath());
                boolean isBinary;
                try {
                    isBinary = FileUtil.isBinaryFile(fileToUpload);
                } catch (IOException ioe) {
                    publish(new UploadErrorMessage(this, dataFile, "Failed to read file: " + fileToUpload.getName()));
                    return null;
                }

                if (isBinary) {
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);
                    logger.debug("File to upload is binary");
                }

                // transfer files
                inputStream = new FileInputStream(fileToUpload);

                logger.info("Starting to upload file: " + fileToUpload.getAbsolutePath());
                ftp.storeFile(fileToUpload.getName(),inputStream);
            } else if (dataFile.isUrl()) {
                URL urlToUpload = dataFile.getUrl();
                logger.info("About to upload file: " + urlToUpload);
                // Treat all URL data as binary

                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                logger.debug("File to upload is binary");

                // transfer files
                inputStream = urlToUpload.openStream();
                ftp.storeFile(fileName,inputStream);

                logger.info("Starting to upload file: " + urlToUpload);
                ftp.storeFile(urlToUpload.getFile(),inputStream);
            }

           // Util.copyStream(inputStream, outputStream, BUFFER_SIZE, CopyStreamEvent.UNKNOWN_STREAM_SIZE, this, true);

            int retryCount = 1;
            logger.info("Checking file size in the server and validating: " + dataFile.getFileName());
            while(!FTPReply.isPositiveCompletion(ftp.sendCommand("size", dataFile.getFile().getName()))
            || Long.parseLong(ftp.getReplyString().split(" ")[1].trim()) != Files.size(Paths.get(dataFile.getFilePath())) )
            {
                if(retryCount>=3){
                    logger.info("Failed uploading 3 times for file: " + dataFile.getFilePath() + " Please try another time");
                    throw new IOException("FTP transfer failure for the file");
                }
                logger.info("Retrying to upload file: " + dataFile.getFilePath() + " Count "+ retryCount);
                Util.copyStream(inputStream, outputStream, BUFFER_SIZE, CopyStreamEvent.UNKNOWN_STREAM_SIZE, this, true);
                retryCount++;
            }
            logger.info("File check done: " + dataFile.getFileName());
            publish(new UploadFileSuccessMessage(this, dataFile));

        } catch (IOException e) {
            if (!this.isCancelled()) {
                logger.error("IOException while uploading file: " + fileName, e);
                if (e instanceof CopyStreamException) {
                    IOException ioe = ((CopyStreamException) e).getIOException();
                    logger.error("CopyStreamException contains IOException: ", ioe);

                    logger.error("Total byte transferred: " + ((CopyStreamException) e).getTotalBytesTransferred());

                    // print ioe stacktrace
                    StackTraceElement[] stackTraceElements = ioe.getStackTrace();
                    for (StackTraceElement stackTraceElement : stackTraceElements) {
                        logger.error(stackTraceElement.toString());
                    }
                }
                publish(new UploadErrorMessage(this, dataFile, "Failed file transfer: " + fileName));
            }
        } finally {
            if (!this.isCancelled()) {
                logger.debug("Freeing ftp resources before finishing");
                releaseResource();
            }
        }

        return null;
    }

    @Override
    protected void cancelled() {
        logger.debug("Freeing ftp resources before cancelling");
        releaseResource();
        publish(new UploadCancelMessage(this, dataFile));
    }

    /**
     * Release ftp upload resources
     */
    private void releaseResource() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                // do nothing
            }
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                // do nothing
            }
        }

        if (ftp != null && ftp.isConnected()) {
            try {
                // logout
                ftp.logout();
                // disconnect
                ftp.disconnect();
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }

    @Override
    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
        publish(new UploadProgressMessage(this, dataFile, streamSize, bytesTransferred));
    }

    @Override
    public void bytesTransferred(CopyStreamEvent event) {
        // do nothing
    }
}
