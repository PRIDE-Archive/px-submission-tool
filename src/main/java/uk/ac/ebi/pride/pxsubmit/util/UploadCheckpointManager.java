package uk.ac.ebi.pride.pxsubmit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.pxsubmit.model.UploadCheckpoint;

import java.io.IOException;
import java.nio.file.*;

/**
 * Manages the upload checkpoint file for crash-recovery and resume support.
 * Checkpoint is stored at ~/.pxsubmit/upload-checkpoint.json.
 *
 * All writes are atomic: data is written to a .tmp file first,
 * then renamed to the target file.
 */
public class UploadCheckpointManager {

    private static final Logger logger = LoggerFactory.getLogger(UploadCheckpointManager.class);

    private static final Path CHECKPOINT_DIR = Paths.get(System.getProperty("user.home"), ".pxsubmit");
    private static final String CHECKPOINT_FILE = "upload-checkpoint.json";
    private static final String CHECKPOINT_TMP = "upload-checkpoint.json.tmp";

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private UploadCheckpointManager() {}

    /**
     * Save checkpoint atomically (write to .tmp, rename).
     */
    public static void save(UploadCheckpoint checkpoint) {
        try {
            Files.createDirectories(CHECKPOINT_DIR);
            Path tmpFile = CHECKPOINT_DIR.resolve(CHECKPOINT_TMP);
            Path targetFile = CHECKPOINT_DIR.resolve(CHECKPOINT_FILE);

            mapper.writeValue(tmpFile.toFile(), checkpoint);
            Files.move(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            logger.debug("Checkpoint saved to {}", targetFile);
        } catch (IOException e) {
            logger.error("Failed to save upload checkpoint", e);
        }
    }

    /**
     * Load checkpoint from disk. Returns null if not found or unreadable.
     */
    public static UploadCheckpoint load() {
        Path file = CHECKPOINT_DIR.resolve(CHECKPOINT_FILE);
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return mapper.readValue(file.toFile(), UploadCheckpoint.class);
        } catch (IOException e) {
            logger.error("Failed to load upload checkpoint", e);
            return null;
        }
    }

    /**
     * Delete the checkpoint file.
     */
    public static void delete() {
        try {
            Path file = CHECKPOINT_DIR.resolve(CHECKPOINT_FILE);
            Files.deleteIfExists(file);
            Files.deleteIfExists(CHECKPOINT_DIR.resolve(CHECKPOINT_TMP));
            logger.info("Upload checkpoint deleted");
        } catch (IOException e) {
            logger.warn("Failed to delete upload checkpoint", e);
        }
    }

    /**
     * Check if a checkpoint file exists.
     */
    public static boolean exists() {
        return Files.exists(CHECKPOINT_DIR.resolve(CHECKPOINT_FILE));
    }

    /**
     * Mark a single file as uploaded in the checkpoint.
     * Loads the checkpoint, adds the file name, saves back atomically.
     */
    public static synchronized void markFileUploaded(String fileName) {
        UploadCheckpoint checkpoint = load();
        if (checkpoint != null) {
            checkpoint.getUploadedFileNames().add(fileName);
            save(checkpoint);
            logger.debug("Marked file as uploaded in checkpoint: {}", fileName);
        }
    }
}
