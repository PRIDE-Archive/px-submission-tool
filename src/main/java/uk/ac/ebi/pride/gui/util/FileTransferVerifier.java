package uk.ac.ebi.pride.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;

import uk.ac.ebi.pride.gui.data.Credentials;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example usage of FileListParser for verifying file transfers
 */
public class FileTransferVerifier {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTransferVerifier.class);
    
    /**
     * Verify file transfer for a given dropbox path
     * 
     * @param dropboxPath the path in the dropbox (e.g., "pride-drop-002/selva_cdsncsd")
     * @param localFiles map of local file names to their sizes in bytes
     * @param credentials user credentials for authentication
     * @return validation result
     */
    public static FileListParser.ValidationResult verifyTransfer(String dropboxPath, Map<String, Long> localFiles, Credentials credentials) throws Exception {
        DesktopContext context = App.getInstance().getDesktopContext();
        String apiBaseUrl = context.getProperty("px.upload.verify.url");
        
        logger.info("Verifying transfer for path: {}", dropboxPath);
        logger.info("Local files to verify: {}", localFiles.size());
        logger.debug("Using API URL: {}", apiBaseUrl);
        
        // Fetch file list from server
        List<FileListParser.FileEntry> remoteFiles = FileListParser.fetchFileList(apiBaseUrl, dropboxPath, credentials);
        logger.info("Remote files found: {}", remoteFiles.size());
        
        // Validate transfer
        FileListParser.ValidationResult result = FileListParser.validateTransfer(localFiles, remoteFiles);
        
        // Log results
        if (result.isValid()) {
            logger.info("Transfer validation PASSED: All {} files transferred correctly", result.getValidFiles().size());
        } else {
            logger.warn("Transfer validation FAILED:");
            logger.warn("  Missing files: {}", result.getMissingFiles().size());
            logger.warn("  Size mismatches: {}", result.getSizeMismatches().size());
            logger.warn("  Extra files: {}", result.getExtraFiles().size());
            
            // Log details of issues
            for (FileListParser.MissingFile missing : result.getMissingFiles()) {
                logger.warn("  Missing: {} (size: {})", missing.getFileName(), missing.getLocalSize());
            }
            for (FileListParser.SizeMismatch mismatch : result.getSizeMismatches()) {
                logger.warn("  Size mismatch: {} (local: {}, remote: {})", 
                    mismatch.getFileName(), mismatch.getLocalSize(), mismatch.getRemoteSize());
            }
            for (FileListParser.ExtraFile extra : result.getExtraFiles()) {
                logger.warn("  Extra file: {} (size: {})", extra.getFileName(), extra.getRemoteSize());
            }
        }
        
        return result;
    }
    
    /**
     * Example usage method
     */
    public static void main(String[] args) {
        // Example: verify transfer for a specific dropbox path
        String dropboxPath = "pride-drop-002/selva_20251023_131936_063df6fb-fb7a-4563-9510-3ab8577f07c1";
        
        // Example local files (you would get this from your local file system)
        Map<String, Long> localFiles = new HashMap<>();
        localFiles.put("Olink5K_Data_npx.parquet", 123456L);
        localFiles.put("sample_data.txt", 7890L);
        
        try {
            // Example credentials (you would get these from the user)
            Credentials credentials = new Credentials("username", "password");
            FileListParser.ValidationResult result = verifyTransfer(dropboxPath, localFiles, credentials);
            
            System.out.println("Validation Result: " + result);
            System.out.println("Valid: " + result.isValid());
            System.out.println("Valid files: " + result.getValidFiles().size());
            System.out.println("Missing files: " + result.getMissingFiles().size());
            System.out.println("Size mismatches: " + result.getSizeMismatches().size());
            System.out.println("Extra files: " + result.getExtraFiles().size());
            
        } catch (Exception e) {
            System.err.println("Verification failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
