package uk.ac.ebi.pride.gui.task;

import com.asperasoft.faspmanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.aspera.PersistedAsperaFileUploader;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.task.ftp.*;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.OSDetector;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by ilias
 */

public class PersistedAsperaUploadTask extends TaskAdapter<Void, UploadMessage> implements TransferListener {

    public static final Logger logger = LoggerFactory.getLogger(AsperaUploadTask.class);

    private SubmissionRecord submissionRecord;
    /**
     * Set contains files need to be submitted along with the folder name
     */
    private Iterator<File> fileToSubmit;

    /**
     * Total file size need to be uploaded
     */
    private long totalFileSize;

    /**
     * finished file count
     */
    private int finishedFileCount;

    /**
     * Constructor used for a new submission
     *
     * @param submissionRecord submission record
     */
    public PersistedAsperaUploadTask(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;
        this.totalFileSize = 0;
        this.finishedFileCount = 0;
    }

    @Override
    protected Void doInBackground() throws Exception {


        // save submission initial progress
        serializeSubmissionReport();

        // prepare for ftp upload
        fileToSubmit = prepareSubmission();

        // upload via aspera
        asperaUpload();

        // wait for aspera to upload
        waitUpload();

        return null;
    }

    private void waitUpload() throws InitializationException, InterruptedException {
        FaspManager faspManager = FaspManager.getSingleton();

        // this is keep the fasp manager running
        while (faspManager.isRunning()) {
            Thread.sleep(30000);
        }
    }

    private void asperaUpload() throws FaspManagerException {

        // choose aspera binary according to operating system
        String ascpLocation = chooseAsperaBinary();
        logger.debug("Aspera binary location {}", ascpLocation);

        // binary location
        File executable = new File(ascpLocation);

        // set aspera connection details
        XferParams defaultTransferParams = getDefaultTransferParams();

        // construct uploader
        PersistedAsperaFileUploader uploader = new PersistedAsperaFileUploader(executable);


        // add transfer listener
        uploader.addTransferListener(this);

        // remote location
        final UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        final DropBoxDetail dropBox = uploadDetail.getDropBox();
        RemoteLocation remoteLocation = new RemoteLocation(uploadDetail.getHost(), dropBox.getUserName(), dropBox.getPassword());

        // start upload
        String session = uploader.startTransferSession(remoteLocation, defaultTransferParams);
        logger.debug("Transfer Session ID: {}", session);
    }

    /**
     * Set the default transfer parameters for this transfer.
     * For supported parameters see class #XferParams
     * For additional descriptions see the Aspera documentation
     * of the command line tool. For example at
     * http://download.asperasoft.com/download/docs/ascp/2.7/html/index.html
     * <p/>
     *
     * @return the default transfer parameters.
     */
    public static XferParams getDefaultTransferParams() {
        DesktopContext appContext = App.getInstance().getDesktopContext();
        XferParams p = new XferParams();
        p.tcpPort = Integer.parseInt(appContext.getProperty("aspera.xfer.tcpPort"));
        p.udpPort = Integer.parseInt(appContext.getProperty("aspera.xfer.udpPort")); // port used for data transfer
        p.targetRateKbps = Integer.parseInt(appContext.getProperty("aspera.xfer.targetRateKbps")); // 10000000 Kbps (= 10 Gbps)
        p.minimumRateKbps = Integer.parseInt(appContext.getProperty("aspera.xfer.minimumRateKbps")); //    100 Kbps
        p.encryption = Encryption.DEFAULT;
        p.overwrite = Overwrite.DIFFERENT;
        p.generateManifest = Manifest.NONE;
        p.policy = Policy.FAIR;
        p.cookie = appContext.getProperty("aspera.xfer.cookie");
        p.token = appContext.getProperty("aspera.xfer.token");
        p.resumeCheck = Resume.SPARSE_CHECKSUM;
        p.preCalculateJobSize = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.preCalculateJobSize"));
        p.createPath = Boolean.parseBoolean(appContext.getProperty("aspera.xfer.createPath"));
        p.persist = true;
        return p;
    }

    private String chooseAsperaBinary() {
        //detect Operating System
        final OSDetector.OS os = OSDetector.getOS();
        final DesktopContext appContext = App.getInstance().getDesktopContext();

        //detect jar directory
        String jarDir = getAbsolutePath();

        // get aspera client binary
        String ascpLocation = "";

        switch (os) {
            case MAC:
                ascpLocation = appContext.getProperty("aspera.client.mac.binary");
                break;
            case LINUX_32:
                ascpLocation = appContext.getProperty("aspera.client.linux32.binary");
                break;
            case LINUX_64:
                ascpLocation = appContext.getProperty("aspera.client.linux64.binary");
                break;
            case WINDOWS:
                ascpLocation = appContext.getProperty("aspera.client.windows.binary");
                break;
            default:
                String msg = "Unsupported platform detected:" + OSDetector.os + " arch: " + OSDetector.arch;
                logger.error(msg);
                publish(new UploadErrorMessage(this, null, msg));
        }

        //concatenate jar directory plus relative ascp binaries directory
        return jarDir + File.separator + ascpLocation;
    }

    /**
     * Get the root path of aspera binary
     *
     * @return root path in string
     */
    private String getAbsolutePath() {
        String jarDir = null;

        //get absolute path including jar filename
        String jarPath = AsperaUploadTask.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        logger.debug("Jar Path: {}", jarPath);

        try {
            String decodedJarPath = URLDecoder.decode(jarPath, "UTF-8");
            //convert String object to File object in order to be able to use getParent()
            File jarFile = new File(decodedJarPath);
            //this is the directory one level above the jar file
            File jarParent = new File(jarFile.getParent());
            // this is the directory two levels above the jar file
            if (!decodedJarPath.endsWith("jar"))
                jarDir = jarParent.getParent();
            else
                jarDir = jarParent.getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            final String msg = "Failed to locate aspera binary";
            logger.error(msg, e);
            publish(new UploadErrorMessage(this, null, msg));
        }

        return jarDir;
    }

    /**
     * Prepare for upload an entire submission
     */
    private Iterator<File> prepareSubmission() {
        logger.debug("Preparing for uploading an entire submission");

        Set<File> files = Collections.synchronizedSet(new LinkedHashSet<File>());

        // add submission summary file
        File submissionFile = createSubmissionFile(); //submission px file creation
        if (submissionFile != null) {
            files.add(submissionFile); //add the submission px file to the upload list
        }

        // prepare for submission
        for (DataFile dataFile : submissionRecord.getSubmission().getDataFiles()) {
            totalFileSize += dataFile.getFile().length();
            if (dataFile.isFile()) {
                files.add(dataFile.getFile());
            }
        }

        return files.iterator();
    }

    /**
     * Create submission file
     *
     * @return boolean true indicates success
     */
    private File createSubmissionFile() {
        try {
            // create a random temporary directory
            SecureRandom random = new SecureRandom();
            File tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + random.nextLong());
            tempDir.mkdir();

            File submissionFile = new File(tempDir.getAbsolutePath() + File.separator + Constant.PX_SUBMISSION_SUMMARY_FILE);

            logger.debug("Create temporary submission summary file : " + submissionFile.getAbsolutePath());

            // write out submission details
            SubmissionFileWriter.write(submissionRecord.getSubmission(), submissionFile);

            return submissionFile;
        } catch (SubmissionFileException ex) {
            String msg = "Failed to create submission file";
            logger.error(msg, ex);
            publish(new UploadErrorMessage(this, null, msg));
        }
        return null;
    }

    private void serializeSubmissionReport() {
        try {
            SubmissionRecordSerializer.serialize(submissionRecord);
        } catch (IOException ioe) {
            logger.error("Failed to save submission record");
        }
    }

    @Override
    protected void cancelled() {
        publish(new UploadStoppedMessage(this, submissionRecord));
    }

    @Override
    public void fileSessionEvent(TransferEvent transferEvent, SessionStats sessionStats, FileInfo fileInfo) {
        FaspManager faspManager = null;
        try {
            faspManager = FaspManager.getSingleton();
        } catch (InitializationException e) {
            FaspManager.destroy();
            publish(new UploadErrorMessage(this, null, "Failed to initialize via Aspera manager"));
        }

        int totalNumOfFiles = submissionRecord.getSubmission().getDataFiles().size();
        UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        String folder = uploadDetail.getFolder();

        switch (transferEvent) {
            case SESSION_START:
                int count = 0;
                while (fileToSubmit.hasNext() && count < 20) {
                    File file = fileToSubmit.next();
                    try {
                        faspManager.addSource(sessionStats.getId(), file.getAbsolutePath(), folder + File.separator + file.getName());
                    } catch (FaspManagerException e) {
                        FaspManager.destroy();
                        publish(new UploadErrorMessage(this, null, "Failed to upload next file via Aspera: " + file.getName()));
                    }
                    count++;
                }
                break;
            case PROGRESS:
                int uploadedNumOfFiles = (int) sessionStats.getFilesComplete();
                logger.debug("Aspera transfer in progress");
                logger.debug("Total files: ");
                logger.debug("Total files: " + totalNumOfFiles);
                logger.debug("Files uploaded: " + uploadedNumOfFiles);
                logger.debug("Total file size " + totalFileSize);
                logger.debug("Uploaded file size " + sessionStats.getTotalTransferredBytes());
                publish(new UploadProgressMessage(this, null, totalFileSize, sessionStats.getTotalTransferredBytes(), totalNumOfFiles, uploadedNumOfFiles));
                break;
            case FILE_STOP:
                if (fileInfo.getState().equals(FileState.FINISHED)) {
                    finishedFileCount++;
                    // last file has finished uploading, add a new one
                    if (fileToSubmit.hasNext()) {
                        File file = fileToSubmit.next();
                        try {
                            faspManager.addSource(sessionStats.getId(), file.getAbsolutePath(), folder + File.separator + file.getName());
                        } catch (FaspManagerException e) {
                            FaspManager.destroy();
                            publish(new UploadErrorMessage(this, null, "Failed to upload next file via Aspera: " + file.getName()));
                        }
                    } else {
                        if (finishedFileCount == totalNumOfFiles + 1) {
                            FaspManager.destroy();
                            publish(new UploadProgressMessage(this, null, totalFileSize, totalFileSize, totalNumOfFiles, totalNumOfFiles));
                            publish(new UploadSuccessMessage(this));
                            logger.debug("Aspera Session Stop");
                        }
                    }
                }
                break;
            case SESSION_STOP:
                FaspManager.destroy();
                publish(new UploadProgressMessage(this, null, totalFileSize, totalFileSize, totalNumOfFiles, totalNumOfFiles));
                publish(new UploadSuccessMessage(this));
                logger.debug("Aspera Session Stop");
                break;
            case SESSION_ERROR:
                logger.debug("Aspera session Error: " + transferEvent.getDescription());
                FaspManager.destroy();
                publish(new UploadErrorMessage(this, null, "Failed to upload via Aspera: " + transferEvent.getDescription()));
                break;
        }
    }
}
