package uk.ac.ebi.pride.gui.aspera;

/**
 * Listener for ascp CLI transfer events (replaces FaspManager TransferListener).
 */
public interface AscpTransferListener {

    void fileSessionEvent(AscpTransferEvent event, AscpSessionStats sessionStats, AscpFileInfo fileInfo);
}
