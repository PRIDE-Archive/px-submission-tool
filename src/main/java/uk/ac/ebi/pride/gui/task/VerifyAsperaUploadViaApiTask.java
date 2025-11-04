package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;
import uk.ac.ebi.pride.gui.util.FileListParser;
import uk.ac.ebi.pride.gui.util.FileTransferVerifier;
import uk.ac.ebi.pride.gui.data.Credentials;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Task to verify Aspera upload completion using the archive-submission-ws API
 * instead of FTP verification.
 * 
 * This replaces VerifyAsperaUploadViaFtpTask with a more reliable API-based approach.
 */
public class VerifyAsperaUploadViaApiTask extends TaskAdapter<Void, Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(VerifyAsperaUploadViaApiTask.class);
    
    private final SubmissionRecord submissionRecord;
    private final Set<File> localFiles;
    private final String remoteFolder;
    
    public VerifyAsperaUploadViaApiTask(SubmissionRecord submissionRecord, Set<File> localFiles, String remoteFolder) {
        this.submissionRecord = submissionRecord;
        this.localFiles = localFiles;
        this.remoteFolder = remoteFolder;
    }
    
    @Override
    protected Void doInBackground() throws Exception {
        // Perform API-based verification
        logger.info("🔍 Starting API verification for {} files in folder: {}", localFiles.size(), remoteFolder);
        
        try {
            // Build map of local files (filename -> size)
            Map<String, Long> localFileMap = new HashMap<>();
            for (File localFile : localFiles) {
                if (localFile.isFile()) {
                    localFileMap.put(localFile.getName(), localFile.length());
                    logger.debug("Local file: {} ({} bytes)", localFile.getName(), localFile.length());
                }
            }
            
            logger.info("📋 Verifying {} local files", localFileMap.size());

            String dropboxPath = remoteFolder;
            logger.info("🎯 Using dropbox path: {}", dropboxPath);
            
            // Get credentials from submission record (logged-in user credentials)
            String username = submissionRecord.getUserName();
            String password = submissionRecord.getPassword();
            Credentials credentials = new Credentials(username, password);
            
            // Verify transfer using the API
            FileListParser.ValidationResult result = FileTransferVerifier.verifyTransfer(dropboxPath, localFileMap, credentials);
            
            if (result.isValid()) {
                logger.info("✅ API verification successful! All {} files verified.", result.getValidFiles().size());
            } else {
                StringBuilder errorMsg = new StringBuilder("Verification failed: ");
                
                if (!result.getMissingFiles().isEmpty()) {
                    errorMsg.append(result.getMissingFiles().size()).append(" missing files");
                }
                if (!result.getSizeMismatches().isEmpty()) {
                    if (errorMsg.length() > "Verification failed: ".length()) {
                        errorMsg.append(", ");
                    }
                    errorMsg.append(result.getSizeMismatches().size()).append(" size mismatches");
                }
                if (!result.getExtraFiles().isEmpty()) {
                    if (errorMsg.length() > "Verification failed: ".length()) {
                        errorMsg.append(", ");
                    }
                    errorMsg.append(result.getExtraFiles().size()).append(" extra files");
                }
                
                logger.error("❌ API verification failed: {}", errorMsg);
                
                // Log detailed information for debugging
                for (FileListParser.MissingFile missing : result.getMissingFiles()) {
                    logger.error("  Missing: {} ({} bytes)", missing.getFileName(), missing.getLocalSize());
                }
                for (FileListParser.SizeMismatch mismatch : result.getSizeMismatches()) {
                    logger.error("  Size mismatch: {} (local: {}, remote: {} bytes)", 
                        mismatch.getFileName(), mismatch.getLocalSize(), mismatch.getRemoteSize());
                }
                for (FileListParser.ExtraFile extra : result.getExtraFiles()) {
                    logger.error("  Extra file: {} ({} bytes)", extra.getFileName(), extra.getRemoteSize());
                }
                
                throw new Exception(errorMsg.toString());
            }
            
        } catch (Exception e) {
            logger.error("❌ API verification failed", e);
            throw e;
        }
        
        return null;
    }
}
