package uk.ac.ebi.pride.gui.aspera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default listener that logs ascp CLI transfer events.
 */
public class DefaultAscpTransferListener implements AscpTransferListener {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAscpTransferListener.class);

    @Override
    public void fileSessionEvent(AscpTransferEvent event, AscpSessionStats sessionStats, AscpFileInfo fileInfo) {
        if (event == AscpTransferEvent.PROGRESS) {
            logger.debug(
                    "ascp progress: files={} file={}",
                    sessionStats != null ? sessionStats.getFilesComplete() : "?",
                    fileInfo != null ? fileInfo.getName() : "?");
        } else {
            logger.info("ascp event: {} session={}", event, sessionStats != null ? sessionStats.getId() : "?");
        }
        if (sessionStats != null && sessionStats.getState() == AscpSessionState.FAILED) {
            logger.error("ascp session failed: {}", sessionStats.getErrorDescription());
        }
    }
}
