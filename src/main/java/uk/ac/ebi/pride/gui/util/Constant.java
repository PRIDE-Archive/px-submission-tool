package uk.ac.ebi.pride.gui.util;

import java.util.regex.Pattern;

/**
 * This class stores all the static constants shared by this application
 *
 * @author Rui Wang
 * @version $Id$
 */
public final class Constant {

    private Constant() {
    }

    /**
     * This section contains all the separators
     */
    public static final String TAB = "\t";

    public static final String COMMA = ",";


    /**
     * Shared strings
     */
    public static final String UNKNOWN = "Unknown";
    public static final String URL = "URL";

    public static final String ADAT = "adat";


    public static final String EMAIL = "email";
    public static final String FTP = "ftp";
    public static final String ASPERA = "aspera";

    public static final String TICKET_ID = "ticketId";

    public static final String PX_TOOL_USER_DIRECTORY = ".px-tool";

    public static final String PX_SUBMISSION_PROGRESS_RECORD = "sub_record.px";

    public static final String PX_SUBMISSION_SUMMARY_FILE = "submission.px";

    public static final Pattern PUBMED_PATTERN = Pattern.compile("^\\d[\\d, ]+\\d$");

    public static final Pattern REANALYSIS_PX_ACC_PATTERN = Pattern.compile("^(PXD\\d{6}[, ]*)+$");

    public static final Pattern PX_ACC_PATTERN = Pattern.compile("^PXD\\d{6}$");

    public static final Pattern PAD_ACC_PATTERN = Pattern.compile("^PAD\\d{6}$");

    public static final Pattern FILENAME_PATTERN = Pattern.compile("^[A-Za-z0-9][-_.A-Za-z0-9]*$");

    public static final String FILENAME_DESCRIPTION =
            "File names must start with a letter or digit and contain only letters, digits, hyphens, underscores, and dots.";

    public static boolean isValidFilename(String filename) {
        return filename != null && FILENAME_PATTERN.matcher(filename).matches();
    }
}
