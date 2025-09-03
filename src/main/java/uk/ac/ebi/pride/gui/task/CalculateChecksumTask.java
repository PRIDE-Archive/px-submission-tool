package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.form.CalculateChecksumDescriptor;
import uk.ac.ebi.pride.gui.task.checksum.ChecksumMessage;
import uk.ac.ebi.pride.gui.util.Hash;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;
import uk.ac.ebi.pride.utilities.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CalculateChecksumTask extends TaskAdapter<Boolean, ChecksumMessage> {

    private static final Logger logger = LoggerFactory.getLogger(CalculateChecksumTask.class);

    private Submission submission;

    public CalculateChecksumTask(Submission submission) {
        this.submission = submission;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        logger.info("=== CalculateChecksumTask STARTED ===");
        logger.info("Submission: {}", submission);
        logger.info("Number of data files: {}", submission.getDataFiles().size());
        
        final int NUMBER_OF_THREADS = Integer.parseInt(System.getProperty("px.checksum.threads.size", "1"));
        logger.info("Using {} threads for checksum calculation", NUMBER_OF_THREADS);
        
        List<DataFile> dataFiles = submission.getDataFiles();
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        List<Future<Tuple<String, String>>> tasks = new ArrayList<>();
        
        logger.info("=== Starting checksum calculation for files ===");
        for (DataFile dataFile : dataFiles) {
            logger.debug("Processing file: {}", dataFile.getFile().getName());
            if (!dataFile.getFile().getName().equals("checksum.txt")) {
                logger.info("Adding checksum calculation task for: {}", dataFile.getFile().getName());
                tasks.add(executor.submit((Callable) () -> {
                    try {
                        logger.debug("Calculating SHA1 checksum for: {}", dataFile.getFile().getName());
                        Tuple<String, String> result = calculateSha1Checksum(dataFile.getFile());
                        logger.info("Checksum calculated successfully for: {} -> {}", dataFile.getFile().getName(), result.getValue());
                        return result;
                    } catch (Exception exception) {
                        logger.error("Error in calculating checksum for file: {}", dataFile.getFile().getName(), exception);
                        return null;
                    }
                }));
            } else {
                logger.info("Skipping checksum calculation for checksum.txt file");
            }
        }
        
        int i = 0;
        int totalNoOfFiles = dataFiles.size() - 1;
        logger.info("Total files to process: {}", totalNoOfFiles);
        
        if (!dataFiles.stream().anyMatch(file -> file.getFileName().equals("checksum.txt"))) {
            logger.error("No checksum.txt present in submission files");
            throw new RuntimeException("No checksum.txt present");
        }
        
        if (totalNoOfFiles == 0) {
            logger.info("No files to process for checksum calculation");
            publish(new ChecksumMessage(this, null, 0, totalNoOfFiles));
            return null;
        }
        
        logger.info("=== Processing checksum results ===");
        Iterator<Future<Tuple<String, String>>> it = tasks.iterator();
        while (it.hasNext()) {
            Future future = it.next();
            if (future.isDone()) {
                try {
                    Tuple<String, String> fileChecksum = (Tuple<String, String>) future.get();
                    publish(new ChecksumMessage(this, fileChecksum, ++i, totalNoOfFiles));
                    logger.info("Published checksum result {}/{}: {} -> {}", i, totalNoOfFiles, 
                               fileChecksum.getKey(), fileChecksum.getValue());
                    it.remove();
                } catch (Exception e) {
                    logger.error("Error getting checksum result from future", e);
                }
            }
            if (!it.hasNext()) {
                it = tasks.iterator();
            }
        }
        
        logger.info("=== CalculateChecksumTask COMPLETED ===");
        logger.info("Processed {} files successfully", i);
        return null;
    }


    private static Tuple<String, String> calculateSha1Checksum(File file) throws IOException {
        String filePath = file.getAbsolutePath();
        logger.debug("Calculating SHA1 checksum for file: {}", filePath);
        
        if (CalculateChecksumDescriptor.checksumCalculatedFiles.containsKey(filePath)) {
            logger.debug("Using cached checksum for: {}", filePath);
            return CalculateChecksumDescriptor.checksumCalculatedFiles.get(filePath);
        }
        
        logger.debug("Calculating new SHA1 checksum for: {}", filePath);
        String checksum = Hash.getSha1Checksum(file);
        logger.debug("SHA1 checksum calculated: {} -> {}", filePath, checksum);
        
        return new Tuple<>(file.getAbsolutePath(), checksum);
    }

}
