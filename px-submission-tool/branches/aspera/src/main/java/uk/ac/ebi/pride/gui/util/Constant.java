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
    public static final String PRIDE_USER_FIRST_NAME = "First Name";
    public static final String PRIDE_USER_LAST_NAME = "Last Name";
    public static final String PRIDE_USER_EMAIL = "Email";
    public static final String PRIDE_USER_AFFILIATION = "Affiliation";


    public static final String EMAIL = "email";
    public static final String FTP = "ftp";
    public static final String ASPERA = "aspera";
    public static final String PRIDE_USER_NAME = "username";
    public static final String PRIDE_PASSWORD = "password";
    public static final String FTP_USER_NAME = "username";
    public static final String FTP_PASSWORD = "password";
    public static final String FTP_FOLDER = "folder";
    public static final String FTP_PORT = "port";
    public static final String SUBMISSION_REFERENCE = "reference";
    public static final String PROTEOMEXCHANGE = "1";
    public static final String SUBMISSION_TYPE = "type";

    public static final String PX_TOOL_USER_DIRECTORY = ".px-tool";

    public static final String PX_SUBMISSION_PROGRESS_RECORD = "sub_record.px";

    public static final String PX_SUBMISSION_SUMMARY_FILE = "submission.px";

    public static final Pattern PUBMED_PATTERN = Pattern.compile("^\\d[\\d, ]+\\d$");

    public static final Pattern REANALYSIS_PX_ACC_PATTERN = Pattern.compile("^(PXD\\d{6}[, ]*)+$");

}
