package uk.ac.ebi.pride.gui.aspera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;

import java.io.File;
import java.util.Collection;

/**
 * Performs Aspera uploads via the bundled ascp CLI (ascp 4.x).
 */
public class AsperaFileUploader {

    private static final Logger logger = LoggerFactory.getLogger(AsperaFileUploader.class);
    private static final DesktopContext appContext = App.getInstance().getDesktopContext();

    private AscpTransferListener listener;
    private final AscpCliUploader cliUploader;
    private String host;
    private String user;
    private String password;
    private AscpTransferConfig transferConfig;

    public AsperaFileUploader(File ascpExecutable) {
        this.listener = new DefaultAscpTransferListener();
        this.transferConfig = AscpTransferConfig.fromAppContext();
        this.cliUploader = new AscpCliUploader(ascpExecutable, transferConfig);
        this.cliUploader.setListener(listener);
        logger.info("Aspera CLI uploader initialized with: {}", ascpExecutable.getAbsolutePath());
    }

    public static AscpTransferConfig defaultTransferConfig() {
        return AscpTransferConfig.fromAppContext();
    }

    public AscpTransferListener getListener() {
        return listener;
    }

    public void setListener(AscpTransferListener listener) {
        this.listener = listener;
        cliUploader.setListener(listener);
    }

    public AscpTransferConfig getTransferConfig() {
        return transferConfig;
    }

    public void setTransferConfig(AscpTransferConfig transferConfig) {
        this.transferConfig = transferConfig != null ? transferConfig : AscpTransferConfig.fromAppContext();
    }

    public void setRemoteLocation(String server, String user, String pass) {
        logger.info(
                "Setting remote location - Server: {}, User: {}, Pass: {}",
                server,
                user,
                pass != null ? "[PROVIDED]" : "[NULL]");
        this.host = server;
        this.user = user;
        this.password = pass;
    }

    public String getRemoteLocation() {
        return user + "@" + host;
    }

    /**
     * Upload files to the remote directory using ascp CLI.
     *
     * @return session id for the started transfer
     */
    public String uploadFiles(Collection<File> filesToUpload, String destinationDirectory)
            throws AscpTransferException {
        if (host == null || user == null) {
            throw new AscpTransferException("Remote host and user must be set before upload");
        }
        logger.info("Starting CLI upload to {}:{} ({})", user, host, destinationDirectory);
        return cliUploader.startTransfer(filesToUpload, host, user, password, destinationDirectory);
    }

    public void cancel() {
        cliUploader.cancel();
    }
}
