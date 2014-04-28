package uk.ac.ebi.pride.gui.util;

import uk.ac.ebi.pride.gui.data.SubmissionRecord;

import java.io.*;

/**
 * For serializing and deserializing SubmissionRecord
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionRecordSerializer {
    public static final String SUBMISSION_RECORD_FILE = System.getProperty("user.home") + File.separator +
            Constant.PX_TOOL_USER_DIRECTORY + File.separator + Constant.PX_SUBMISSION_PROGRESS_RECORD;

    /**
     * Serialize a submission record
     */
    public static void serialize(SubmissionRecord submissionRecord) throws IOException {
        FileOutputStream fos = new FileOutputStream(SUBMISSION_RECORD_FILE);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(submissionRecord);
        out.flush();
    }

    /**
     * Whether this is an existing submission record
     */
    public static boolean hasSubmissionRecord() {
        File serializedSubmissionRecord = new File(SUBMISSION_RECORD_FILE);
        return serializedSubmissionRecord.exists();
    }

    /**
     * Remove submission record from default user space
     */
    public static boolean remove() {
        File serializedSubmissionRecord = new File(SUBMISSION_RECORD_FILE);
        return !serializedSubmissionRecord.exists() || serializedSubmissionRecord.delete();
    }

    /**
     * Deserialize a submission record
     */
    public static SubmissionRecord deserialize() throws IOException, ClassNotFoundException {
        File serializedSubmissionRecord = new File(SUBMISSION_RECORD_FILE);
        if (serializedSubmissionRecord.exists()) {
            FileInputStream fis = new FileInputStream(serializedSubmissionRecord);
            ObjectInputStream ois = new ObjectInputStream(fis);
            return (SubmissionRecord) ois.readObject();
        } else {
            return null;
        }

    }
}
