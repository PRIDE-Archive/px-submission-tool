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
        final int NUMBER_OF_THREADS = Integer.parseInt(System.getProperty("px.checksum.threads.size", "1"));
        List<DataFile> dataFiles = submission.getDataFiles();
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        List<Future<Tuple<String, String>>> tasks = new ArrayList<>();
        for (DataFile dataFile : dataFiles) {
            if (!dataFile.getFile().getName().equals("checksum.txt")) {
                tasks.add(executor.submit(new Callable() {
                    public Tuple<String, String> call() throws Exception {
                        try {
                            return calculateSha1Checksum(dataFile.getFile());
                        } catch (Exception exception) {
                            logger.error("Error in calculating checksum for file " + dataFile.getFile().getName());
                            return null;
                        }
                    }
                }));
            }
        }
        int i = 0;
        int totalNoOfFiles = dataFiles.size();
        Iterator<Future<Tuple<String, String>>> it = tasks.iterator();
        while (it.hasNext()) {
            Future future = it.next();
            if (future.isDone()) {
                Tuple<String, String> fileChecksum = (Tuple<String, String>) future.get();
                publish(new ChecksumMessage(this, fileChecksum, ++i, totalNoOfFiles - 1));
                it.remove();
            }
            if (!it.hasNext()) {
                it = tasks.iterator();
            }
        }
        return null;
    }


    private static Tuple<String, String> calculateSha1Checksum(File file) throws IOException {
        String filePath = file.getAbsolutePath();
        if (CalculateChecksumDescriptor.checksumCalculatedFiles.containsKey(filePath)) {
            return CalculateChecksumDescriptor.checksumCalculatedFiles.get(filePath);
        }
        return new Tuple<>(file.getAbsolutePath(), Hash.getSha1Checksum(file));
    }

}
