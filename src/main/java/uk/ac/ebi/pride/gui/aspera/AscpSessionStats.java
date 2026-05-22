package uk.ac.ebi.pride.gui.aspera;

/**
 * Aggregate session statistics for an ascp CLI transfer.
 */
public class AscpSessionStats {

    private final String id;
    private final long filesComplete;
    private final long totalTransferredBytes;
    private final long targetRateKbps;
    private final long minRateKbps;
    private final long elapsedUSec;
    private final AscpSessionState state;
    private final String errorDescription;
    private final int errorCode;

    public AscpSessionStats(
            String id,
            long filesComplete,
            long totalTransferredBytes,
            long targetRateKbps,
            long minRateKbps,
            long elapsedUSec,
            AscpSessionState state) {
        this(id, filesComplete, totalTransferredBytes, targetRateKbps, minRateKbps, elapsedUSec, state, null, 0);
    }

    public AscpSessionStats(
            String id,
            long filesComplete,
            long totalTransferredBytes,
            long targetRateKbps,
            long minRateKbps,
            long elapsedUSec,
            AscpSessionState state,
            String errorDescription,
            int errorCode) {
        this.id = id;
        this.filesComplete = filesComplete;
        this.totalTransferredBytes = totalTransferredBytes;
        this.targetRateKbps = targetRateKbps;
        this.minRateKbps = minRateKbps;
        this.elapsedUSec = elapsedUSec;
        this.state = state;
        this.errorDescription = errorDescription;
        this.errorCode = errorCode;
    }

    public String getId() {
        return id;
    }

    public long getFilesComplete() {
        return filesComplete;
    }

    public long getTotalTransferredBytes() {
        return totalTransferredBytes;
    }

    public long getTargetRateKbps() {
        return targetRateKbps;
    }

    public long getMinRateKbps() {
        return minRateKbps;
    }

    public long getElapsedUSec() {
        return elapsedUSec;
    }

    public AscpSessionState getState() {
        return state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isRemote() {
        return true;
    }
}
