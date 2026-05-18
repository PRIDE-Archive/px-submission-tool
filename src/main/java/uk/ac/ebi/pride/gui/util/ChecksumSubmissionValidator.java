package uk.ac.ebi.pride.gui.util;

import uk.ac.ebi.pride.data.model.DataFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates that the checksum manifest file ({@link Constant#CHECKSUM_FILE_NAME}) lists exactly
 * the submission's selected data files (no missing entries and no extra lines for other files).
 */
public final class ChecksumSubmissionValidator {

    public static final class Result {
        private final List<String> missingInChecksum;
        private final List<String> extraInChecksum;
        private final boolean contentValidated;

        public Result(List<String> missingInChecksum, List<String> extraInChecksum, boolean contentValidated) {
            this.missingInChecksum = Collections.unmodifiableList(new ArrayList<>(missingInChecksum));
            this.extraInChecksum = Collections.unmodifiableList(new ArrayList<>(extraInChecksum));
            this.contentValidated = contentValidated;
        }

        /**
         * True when the checksum file was read and compared to the selected files.
         * When false, {@link #isValid()} is false and missing/extra lists are empty (no line-by-line validation ran).
         */
        public boolean wasContentValidated() {
            return contentValidated;
        }

        public boolean isValid() {
            return contentValidated && missingInChecksum.isEmpty() && extraInChecksum.isEmpty();
        }

        public List<String> getMissingInChecksum() {
            return missingInChecksum;
        }

        public List<String> getExtraInChecksum() {
            return extraInChecksum;
        }
    }

    public static Result validate(List<DataFile> dataFiles, File checksumFile, String checksumDataFileName)
            throws IOException {
        List<DataFile> payloadFiles = new ArrayList<>();
        for (DataFile df : dataFiles) {
            if (!checksumDataFileName.equals(df.getFileName())) {
                payloadFiles.add(df);
            }
        }

        if (checksumFile == null || !checksumFile.exists() || !checksumFile.canRead()) {
            return new Result(Collections.emptyList(), Collections.emptyList(), false);
        }

        List<String> lineTokens = readFirstColumnTokens(checksumFile);

        List<String> missing = new ArrayList<>();
        for (DataFile df : payloadFiles) {
            boolean matched = false;
            for (String token : lineTokens) {
                if (matchesToken(token, df)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                missing.add(df.getFileName());
            }
        }

        Set<String> extras = new LinkedHashSet<>();
        for (String token : lineTokens) {
            if (token == null || token.isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (DataFile df : payloadFiles) {
                if (matchesToken(token, df)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                extras.add(token);
            }
        }
        return new Result(missing, new ArrayList<>(extras), true);
    }

    private static List<String> readFirstColumnTokens(File checksumFile) throws IOException {
        List<String> tokens = new ArrayList<>();
        List<String> lines = java.nio.file.Files.readAllLines(checksumFile.toPath(), Charset.defaultCharset());
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String firstColumn = extractFirstColumn(trimmed);
            if (firstColumn != null && !firstColumn.isEmpty()) {
                tokens.add(firstColumn);
            }
        }
        return tokens;
    }

    /**
     * First column is path/hash prefix. Tool-written lines use TAB before the checksum; paths may contain spaces.
     */
    private static String extractFirstColumn(String line) {
        int tab = line.indexOf('\t');
        if (tab >= 0) {
            return line.substring(0, tab).trim();
        }
        String[] parts = line.split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }

    static boolean matchesToken(String token, DataFile df) {
        String fileName = df.getFileName();
        if (fileName == null) {
            return false;
        }
        if (token.equals(fileName)) {
            return true;
        }
        String tokenBase = new File(token).getName();
        if (tokenBase.equals(fileName)) {
            return true;
        }
        try {
            String fp = df.getFilePath();
            if (fp != null && !fp.isEmpty()) {
                if (token.equals(fp)) {
                    return true;
                }
                if (tokenBase.equals(new File(fp).getName())) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        File f = df.getFile();
        if (f != null) {
            try {
                if (token.equals(f.getAbsolutePath()) || token.equals(f.getPath())) {
                    return true;
                }
                String canon = f.getCanonicalPath();
                if (token.equals(canon)) {
                    return true;
                }
                File tokenFile = new File(token);
                if (tokenFile.isAbsolute() && tokenFile.getCanonicalPath().equals(canon)) {
                    return true;
                }
            } catch (IOException ignored) {
                return token.equals(f.getAbsolutePath());
            }
        }
        return false;
    }

    private ChecksumSubmissionValidator() {
    }
}
