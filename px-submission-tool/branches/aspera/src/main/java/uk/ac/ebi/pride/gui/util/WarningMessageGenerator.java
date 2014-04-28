package uk.ac.ebi.pride.gui.util;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;

import javax.swing.*;
import java.util.List;

/**
 * @author Rui Wang
 * @version $Id$
 */
public final class WarningMessageGenerator {

    public static String getInvalidPRIDEXMLFileWarning(List<DataFile> prideXmlFiles) {
        StringBuilder errMsg = new StringBuilder();
        errMsg.append("<html>");
        errMsg.append("<b>The following PRIDE XML don't have protein identifications</b><br/>");
        for (DataFile prideXmlFile : prideXmlFiles) {
            errMsg.append("<li>");
            errMsg.append(prideXmlFile.getFile().getName());
            errMsg.append("</li>");
        }
        errMsg.append("</html>");

        return errMsg.toString();
    }

    public static String getInvalidResultFileWarning() {
        return "<html>" + "<b>Invalid result file detected, please submit either PRIDE XML or mzIdentML</b><br/>" + "</html>";
    }

    public static String getMultipleResultFileFormatWarning() {

        return "<html>" + "<b>Both PRIDE XML and mzIdentML detected, please submit one result file format only</b><br/>" + "</html>";
    }

    public static String getInvalidMzIdentMLVersionWarning(List<DataFile> mzIdentMLFiles) {
        StringBuilder errMsg = new StringBuilder();
        errMsg.append("<html>");
        errMsg.append("<b>The following mzIdentML are not version  1.1.0 </b><br/>");
        for (DataFile mzIdentMLFile : mzIdentMLFiles) {
            errMsg.append("<li>");
            errMsg.append(mzIdentMLFile.getFile().getName());
            errMsg.append("</li>");
        }
        errMsg.append("</html>");

        return errMsg.toString();
    }

    public static String getMzIdentMLPeakListFilWarning() {

        return "<html>" + "<b>Please add the following spectrum files, they are referenced by your mzIdentML files</b><br/>" + "</html>";
    }

    /**
     * Show a warning dialog, alert user that they should use PRIDE Converter to convert search output to PRIDE XML files.
     *
     * @return true means continue and ignore warning, false means stop at the current step
     */
    public static boolean showSupportedSearchFileWarning() {
        int n = JOptionPane.showConfirmDialog(((App) App.getInstance()).getMainFrame(),
                "<html>Your search engine output can be converted to PRIDE XML using PRIDE Converter 2 (http://code.google.com/p/pride-converter-2/).<br/>" +
                        "Once converted, a complete ProteomeXchagne submission can be made instead. <br/><br/>" +
                        "<b>Would you like to continue with the current partial submission?</b><html>",
                "Convertible Search Engine Output Detected",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        return n == 0;
    }

    public static String getResultFileWarning() {

        return "<html>" + "<b>Result files found in your partial submission.</b><br/>" + "You can either go to the previous step to select the complete submission option or simply remove these files." + "</html>";
    }

    /**
     * Show warning for the raw only submission, dta, mgf, ms2 or pkl files should be assigne to PEAK type
     */
    public static String getUnsupportedRawFileWarning() {

        // Create the balloon tip
        return "<html>" + "<b>Unsupported raw files found</b><br/>" + "dta, mgf, ms2 or pkl files should be assigned with the PEAK type." + "</html>";
    }

    /**
     * Show warning for submitting submission.px
     */
    public static String getSubmissionPxWarning() {

        // Create the balloon tip
        return "<html>" + "<b>Unsupported file found</b><br/>" + "Please remove submission.px file from the upload file list" + "</html>";
    }

    /**
     * Show warning for the raw only submission, it should contain result files or search files
     */
    public static String getRawOnlyWarning() {

        // Create the balloon tip
        return "<html>" + "<b>RESULT files or SEARCH files found in your raw files only submission.</b><br/>" + "You can either go to the previous step to select a different submission option or simply remove these files." + "</html>";
    }

    /**
     * Show warning of invalid files
     */
    public static String getInvalidFileWarning(int numOfInvalidFiles) {

        return String.valueOf(numOfInvalidFiles) + " invalid files detected, the file must exist and not empty.";
    }

    /**
     * Show warning for missing files
     */
    public static String getMissedFileWarning(SubmissionType submissionType,
                                              boolean hasResultFile,
                                              boolean hasSearchFile,
                                              boolean hasRawFile) {
        StringBuilder errMsg = new StringBuilder();
        errMsg.append("<html>");
        errMsg.append("<b>Please make sure you have the following information:</b><br/>");

        if (submissionType.equals(SubmissionType.COMPLETE) && !hasResultFile) {
            errMsg.append("<li>");
            errMsg.append("PRIDE XML or mzIdentML (plus spectrum files)");
            errMsg.append("</li>");
        }

        if (submissionType.equals(SubmissionType.PARTIAL) && !hasSearchFile) {
            errMsg.append("<li>");
            errMsg.append("Search engine output, such as: identification results produced by the analysis tool of your choice");
            errMsg.append("</li>");
        }

        if (!hasRawFile) {
            errMsg.append("<li>");
            errMsg.append("Raw instrument output files");
            errMsg.append("</li>");
        }

        errMsg.append("</html>");

        return errMsg.toString();
    }

    /**
     * Show warning for mixed supported and unsupported result files
     */
    public static String getUnsupportedResultFileWarning() {

        return "<html>" + "<b>Unsupported result files found</b><br/>" + "Please only add supported result files (PRIDE XML or mzIdentML)" + "</html>";
    }

    public static String getUnsupportedSearchFileWarning() {

        return "<html>" + "<b>Unsupported search files found</b><br/>" + "Please choose complete submission option if you have either PRIDE XML or mzIdentML" + "</html>";
    }
}
