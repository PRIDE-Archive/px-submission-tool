package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.task.checksum.ChecksumMessage;
import uk.ac.ebi.pride.gui.util.Hash;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;
import uk.ac.ebi.pride.utilities.util.Tuple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CalculateChecksumTask extends TaskAdapter<Boolean, ChecksumMessage> {

    private static final Integer BUFFER_SIZE = 2048;
    private Submission submission;

    public CalculateChecksumTask(Submission submission) {
        this.submission = submission;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        final int NUMBER_OF_THREADS = Integer.parseInt(System.getProperty("px.checksum.threads.size", "1"));
        List<DataFile> dataFiles = submission.getDataFiles();
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        List<Future<Tuple<String,String>>> tasks = new ArrayList<>();
        for (DataFile dataFile : dataFiles) {
            tasks.add(executor.submit(new Callable() {
                public Tuple<String,String> call() throws Exception {
                    return calculateSha1Checksum(dataFile.getFile());
                }
            }));
        }
        int i = 0;
        int totalNoOfFiles = dataFiles.size();
        Iterator<Future<Tuple<String,String>>> it = tasks.iterator();
        while (it.hasNext()) {
            Future future = it.next();
            if (future.isDone()) {
                Tuple<String,String> fileChecksum = (Tuple<String,String>) future.get();
                publish(new ChecksumMessage(this, fileChecksum, ++i, totalNoOfFiles));
                it.remove();
            }
            if (!it.hasNext()) {
                it = tasks.iterator();
            }
        }
        return null;
    }


    private static Tuple<String,String> calculateSha1Checksum(File file) throws IOException {
        byte[] bytesRead = new byte[BUFFER_SIZE];
        final MessageDigest inputStreamMessageDigest = Hash.getSha1();
        final DigestInputStream digestInputStream = new DigestInputStream(new FileInputStream(file), inputStreamMessageDigest);
        while (digestInputStream.read(bytesRead) != -1) ;
        return new Tuple<>(file.getAbsolutePath(),Hash.normalize(inputStreamMessageDigest));
    }



}
