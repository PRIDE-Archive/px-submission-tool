package uk.ac.ebi.pride.gui.aspera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks active ascp CLI transfers (replaces FaspManager running-state checks).
 */
public final class AscpTransferManager {

    private static final Logger logger = LoggerFactory.getLogger(AscpTransferManager.class);
    private static final AscpTransferManager INSTANCE = new AscpTransferManager();
    private final AtomicInteger activeTransfers = new AtomicInteger(0);

    private AscpTransferManager() {}

    public static AscpTransferManager getInstance() {
        return INSTANCE;
    }

    void transferStarted() {
        int count = activeTransfers.incrementAndGet();
        logger.debug("Active ascp transfers: {}", count);
    }

    void transferFinished() {
        int count = activeTransfers.decrementAndGet();
        logger.debug("Active ascp transfers: {}", Math.max(0, count));
    }

    public boolean isRunning() {
        return activeTransfers.get() > 0;
    }
}
