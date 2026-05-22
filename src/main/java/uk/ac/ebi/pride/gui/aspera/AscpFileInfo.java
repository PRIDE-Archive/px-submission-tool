package uk.ac.ebi.pride.gui.aspera;

/**
 * Per-file transfer status derived from ascp CLI output.
 */
public class AscpFileInfo {

    private final String name;
    private final AscpFileState state;
    private final long writtenBytes;
    private final long sizeBytes;
    private final String errDescription;
    private final int errCode;

    public AscpFileInfo(String name, AscpFileState state, long writtenBytes, long sizeBytes) {
        this(name, state, writtenBytes, sizeBytes, null, 0);
    }

    public AscpFileInfo(
            String name,
            AscpFileState state,
            long writtenBytes,
            long sizeBytes,
            String errDescription,
            int errCode) {
        this.name = name;
        this.state = state;
        this.writtenBytes = writtenBytes;
        this.sizeBytes = sizeBytes;
        this.errDescription = errDescription;
        this.errCode = errCode;
    }

    public String getName() {
        return name;
    }

    public AscpFileState getState() {
        return state;
    }

    public long getWrittenBytes() {
        return writtenBytes;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getErrDescription() {
        return errDescription;
    }

    public int getErrCode() {
        return errCode;
    }
}
