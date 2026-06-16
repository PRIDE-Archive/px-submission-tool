package uk.ac.ebi.pride.pxsubmit.util;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates upload file names against PRIDE submission naming rules.
 */
public final class FileNameValidator {

    public static final String SUBMISSION_FILE_NAME_RULE =
            "File names can only contain letters, numbers, periods for extensions, " +
            "underscores (_) and hyphens (-). Spaces and other special characters are not allowed.";

    private static final Pattern VALID_SUBMISSION_FILE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

    private FileNameValidator() {
    }

    public static boolean isValidSubmissionFileName(File file) {
        return file != null && isValidSubmissionFileName(file.getName());
    }

    public static boolean isValidSubmissionFileName(String fileName) {
        return fileName != null
                && !fileName.isBlank()
                && !fileName.equals(".")
                && !fileName.equals("..")
                && VALID_SUBMISSION_FILE_NAME.matcher(fileName).matches();
    }

    public static List<File> findInvalidSubmissionFileNames(Collection<File> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .filter(file -> !isValidSubmissionFileName(file))
                .toList();
    }
}
