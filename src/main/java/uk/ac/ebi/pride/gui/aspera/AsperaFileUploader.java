package uk.ac.ebi.pride.gui.aspera;

import com.asperasoft.faspmanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;

import java.io.File;
import java.util.Collection;

/**
 * Class to perform a file upload using Aspera.
 *
 * @author Florian Reisinger
 * @since 0.1
 */
public class AsperaFileUploader {

    private static final Logger logger = LoggerFactory.getLogger(AsperaFileUploader.class);
    static final DesktopContext appContext = App.getInstance().getDesktopContext();
    /**
     * The default TransferListener to listen to transfer events
     * raised by the FaspManager during a transfer.
     */
    private TransferListener listener;
    /**
     * The server settings: host + user + password
     */
    private RemoteLocation remoteLocation;

    private String ebiHost = appContext.getProperty("aspera.EBI.host");
    private String ebiUser = appContext.getProperty("aspera.EBI.user");
    private String ebiPass = appContext.getProperty("aspera.EBI.password");

    private RemoteLocation pridePublicLocation = new RemoteLocation(ebiHost, ebiUser, ebiPass);
    /**
     * The default parameters to use for a file transfer.
     */
    private XferParams transferParameters;

    /**
     * Set the default transfer parameters for this transfer.
     * For supported parameters see class #XferParams
     * For additional descriptions see the Aspera documentation
     * of the command line tool. For example at
     * http://download.asperasoft.com/download/docs/ascp/2.7/html/index.html
     * <p/>
     * todo: externalize these parameters, they should not be hard coded in the class, try to use property file instead
     *
     * @return the default transfer parameters.
     */
    public static XferParams defaultTransferParams() {
        XferParams p = new XferParams();
        p.tcpPort = Integer.parseInt(appContext.getProperty("xfer.tcpPort"));
        p.udpPort = Integer.parseInt(appContext.getProperty("xfer.udpPort")); // port used for data transfer
        p.targetRateKbps = Integer.parseInt(appContext.getProperty("xfer.targetRateKbps")); // 10000000 Kbps (= 10 Gbps)
        p.minimumRateKbps = Integer.parseInt(appContext.getProperty("xfer.minimumRateKbps")); //    100 Kbps
        p.encryption = Encryption.DEFAULT;
        p.overwrite = Overwrite.DIFFERENT;
        p.generateManifest = Manifest.NONE;
        p.policy = Policy.FAIR;
        p.cookie = appContext.getProperty("xfer.cookie");
        p.token = appContext.getProperty("xfer.token");
        p.resumeCheck = Resume.SPARSE_CHECKSUM;
        p.preCalculateJobSize = Boolean.parseBoolean(appContext.getProperty("xfer.preCalculateJobSize"));
        p.createPath = Boolean.parseBoolean(appContext.getProperty("xfer.createPath"));
        return p;
    }

    /**
     * Configure the fasp Manager with the path to the Aspera executable contained in this distribution.
     *
     * @param ascpExecutable the ascp executable file.
     * @throws InitializationException if the initialization with the provided path failed.
     */
    public AsperaFileUploader(File ascpExecutable) throws FaspManagerException {
        // set default listener
        this.listener = new DefaultAsperaTransferListener();

        // set default transfer parameters
        this.transferParameters = defaultTransferParams();

        // set the location of the ascp executable
        Environment.setFasp2ScpPath(getAscpPath(ascpExecutable));

        // set the default listener
        FaspManager.getSingleton().addListener(listener);
    }

    private String getAscpPath(File excutable) {
        if (excutable == null || !excutable.exists()) {
            throw new IllegalArgumentException("Specified ascp executable does not exist.");
        }
        // ToDo: perform other checks
        // check if expected file is in specified location

        logger.info("Aspera executable location: " + excutable);
        return excutable.getAbsolutePath();
    }

    public TransferListener getListener() {
        return listener;
    }

    /**
     * Set the TransferListener.
     * This will also set the listener on the FaspManager used to control the transfer.
     *
     * @param listener the TransferListener used to monitor the transfer (listen to the FaspManagerS events).
     * @throws FaspManagerException if the addition of the listener to the FaspManager did not succeed.
     */
    public void setListener(TransferListener listener) throws FaspManagerException {
        // overwrite the default listener
        FaspManager.getSingleton().removeListener(this.listener);
        this.listener = listener;
        FaspManager.getSingleton().addListener(listener);
    }

    public XferParams getTransferParameters() {
        return transferParameters;
    }

    public void setTransferParameters(XferParams transferParameters) {
        this.transferParameters = transferParameters;
    }

    public String getRemoteLocation() {

        return remoteLocation.toString();
    }

    public void setRemoteLocation(String server, String user, String pass) {
        this.remoteLocation = new RemoteLocation(server, user, pass);
    }

    /**
     * Method to perform a Aspera file upload.
     * To override the server and the transfer parameters see the respective getters and setters.
     *
     * @param filesToUpload        the List of Files that should be uploaded.
     * @param destinationDirectory the destination directory on the server where to put the files.
     * @return the Job ID of this transfer job.
     * @throws FaspManagerException in case of errors in the initialisation process or during the transfer.
     * @see #setRemoteLocation(String, String, String)
     * @see #setTransferParameters(com.asperasoft.faspmanager.XferParams)
     */
    public String uploadFiles(Collection<File> filesToUpload, String destinationDirectory) throws FaspManagerException {
        // set all Files as local resources to be uploaded
        LocalLocation localFiles = new LocalLocation();
        for (File file : filesToUpload) {
            localFiles.addPath(file.getAbsolutePath());
        }

        // define the destination on the server
        remoteLocation.clear(); // clear all path, we only want to allow the one specified for this method!
        remoteLocation.addPath(destinationDirectory);

        // compile the transfer order
        TransferOrder order = new TransferOrder(localFiles, remoteLocation, transferParameters);

        /* Submit the job for transfer */
        return FaspManager.getSingleton().startTransfer(order);
    }

    private String downloadFiles(String[] remoteSourcePaths, String localDestinationDirPath) throws FaspManagerException {

        pridePublicLocation.clear();
        for (String path : remoteSourcePaths) {
            pridePublicLocation.addPath(path);
        }

        LocalLocation localDestination = new LocalLocation();
        localDestination.addPath(localDestinationDirPath);

        TransferOrder order = new TransferOrder(pridePublicLocation, localDestination, transferParameters);

        return FaspManager.getSingleton().startTransfer(order);
    }
}
