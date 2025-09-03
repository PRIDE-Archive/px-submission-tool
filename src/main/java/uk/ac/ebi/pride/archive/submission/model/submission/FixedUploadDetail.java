package uk.ac.ebi.pride.archive.submission.model.submission;

/**
 * Fixed version of UploadDetail that overrides the problematic hashCode method
 * to prevent NullPointerException when dropBox is null
 */
public class FixedUploadDetail extends UploadDetail {
    
    public FixedUploadDetail(UploadMethod method, String host, int port, String folder, DropBoxDetail dropBox) {
        super(method, host, port, folder, dropBox);
    }
    
    @Override
    public int hashCode() {
        // Safe hashCode implementation that handles null dropBox
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getMethod() == null) ? 0 : getMethod().hashCode());
        result = prime * result + ((getHost() == null) ? 0 : getHost().hashCode());
        result = prime * result + getPort();
        result = prime * result + ((getFolder() == null) ? 0 : getFolder().hashCode());
        // Safely handle dropBox - only call hashCode if not null
        if (getDropBox() != null) {
            result = prime * result + getDropBox().hashCode();
        }
        return result;
    }
}
