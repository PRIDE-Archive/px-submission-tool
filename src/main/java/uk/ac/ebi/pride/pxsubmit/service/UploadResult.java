package uk.ac.ebi.pride.pxsubmit.service;

/**
 * Unified upload result used by both FTP and Aspera upload services.
 */
public class UploadResult {
    private final boolean success;
    private final int successCount;
    private final int failureCount;
    private final long bytesUploaded;
    private final String errorMessage;

    private UploadResult(boolean success, int successCount, int failureCount,
                         long bytesUploaded, String errorMessage) {
        this.success = success;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.bytesUploaded = bytesUploaded;
        this.errorMessage = errorMessage;
    }

    /** Create a result for FTP uploads (tracks bytes uploaded). */
    public static UploadResult ftpResult(boolean success, int successCount,
                                         int failureCount, long bytesUploaded) {
        return new UploadResult(success, successCount, failureCount, bytesUploaded, null);
    }

    /** Create a result for Aspera uploads (tracks error message). */
    public static UploadResult asperaResult(boolean success, int successCount,
                                            int failureCount, String errorMessage) {
        return new UploadResult(success, successCount, failureCount, 0, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public long getBytesUploaded() { return bytesUploaded; }
    public String getErrorMessage() { return errorMessage; }
}
