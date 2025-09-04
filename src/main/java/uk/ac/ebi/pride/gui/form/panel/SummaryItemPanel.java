package uk.ac.ebi.pride.gui.form.panel;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.gui.form.SummaryDescriptor;
import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.form.dialog.ValidationProgressDialog;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


/**
 * SummaryItemPanel displays a list of file counts for a submission
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SummaryItemPanel extends ContextAwarePanel
        implements PropertyChangeListener, ActionListener {
    private static final Logger logger = LoggerFactory.getLogger(SummaryItemPanel.class);
    private static final float DEFAULT_TITLE_FONT_SIZE = 13f;
    private static final String EXPORT_SUMMARY_ACTION = "exportSummary";
    private static final String VALIDATE_ACTION = "validate";
    private static final String SINGLE_RESULT_FILE = "single";
    private static final String MULTIPLE_RESULT_FILES = "multiple";
    private static final boolean IS_FAST_VALIDATION_ENABLED = true; // we decided to use only FAST_VALIDATION

    Submission submission;
    SubmissionTypeConstants submissionType;

    public static File checksumFile;

    public SummaryItemPanel() {
        // get submission details
        submission = appContext.getSubmissionRecord().getSubmission();
        submissionType = submission.getProjectMetaData().getSubmissionType();
        
        // Always create checksum file in current directory of the tool
        checksumFile = new File(appContext.getProperty("checksum.filename"));
        
        // If checksum file already exists, rename it to backup and create new one
        if (checksumFile.exists()) {
            try {
                // Create backup filename with timestamp
                String checksumFilename = appContext.getProperty("checksum.filename");
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                String nameWithoutExt = checksumFilename.substring(0, checksumFilename.lastIndexOf('.'));
                String extension = checksumFilename.substring(checksumFilename.lastIndexOf('.'));
                String backupFilename = nameWithoutExt + "_backup_" + timestamp + extension;
                File backupFile = new File(backupFilename);
                
                // Rename existing file to backup
                if (checksumFile.renameTo(backupFile)) {
                    logger.info("Renamed existing checksum file to backup: {}", backupFile.getAbsolutePath());
                } else {
                    logger.warn("Could not rename existing checksum file to backup: {}", checksumFile.getAbsolutePath());
                    // If rename fails, try to delete the existing file
                    if (!checksumFile.delete()) {
                        logger.error("Could not delete existing checksum file: {}", checksumFile.getAbsolutePath());
                        throw new IOException("Cannot remove existing checksum file: " + checksumFile.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to handle existing checksum file", e);
                // Show error to user
                JOptionPane.showMessageDialog(app.getMainFrame(),
                    "Failed to create new checksum file. Please ensure you have write permissions in the current directory.\n\n" +
                    "Error: " + e.getMessage(),
                    "Checksum File Creation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        addChecksumFile();
        appContext.addPropertyChangeListener(this);
        populateSummaryItemPanel();
    }

    /**
     * Populate summary item panel
     */
    private void populateSummaryItemPanel() {
        // remove all existing components
        this.removeAll();

        // set layout
        this.setLayout(new BorderLayout());

        JPanel summaryPanel = new JPanel();
        GridLayout layout = new GridLayout(2, 3);
        layout.setVgap(10);
        summaryPanel.setLayout(layout);


        // count different type of submission files
        int[] submissionFileCount = countSubmissionFiles();

        // add total count
        summaryPanel.add(
                createCountLabel(appContext.getProperty("total.file.title"), submissionFileCount[0]));

        // add result file count
        summaryPanel.add(
                createCountLabel(appContext.getProperty("result.file.title"), submissionFileCount[1]));

        // add raw file count
        summaryPanel.add(
                createCountLabel(appContext.getProperty("raw.file.title"), submissionFileCount[2]));

        // add peak file count
        summaryPanel.add(
                createCountLabel(appContext.getProperty("peak.file.title"), submissionFileCount[3]));

        // add search result file count
        summaryPanel.add(
                createCountLabel(appContext.getProperty("search.file.title"), submissionFileCount[4]));

        // add other file count
        summaryPanel.add(
                createCountLabel(appContext.getProperty("other.file.title"), submissionFileCount[5]));

        this.add(summaryPanel, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(2, 1));


        // add export summary button
        JButton exportSummaryButton =
                new JButton(appContext.getProperty("export.summary.button.label"));
        BalloonTipUtil.createBalloonTooltip(
                exportSummaryButton, appContext.getProperty("export.summary.button.tooltip"));
        exportSummaryButton.setActionCommand(EXPORT_SUMMARY_ACTION);
        exportSummaryButton.addActionListener(this);
        buttonsPanel.add(exportSummaryButton);

        // add validation button
//        JButton validationButton = new JButton(appContext.getProperty("summary.validate.button.title"));
//        BalloonTipUtil.createBalloonTooltip(
//                validationButton, appContext.getProperty("summary.validate.button.tooltip"));
//        validationButton.setActionCommand(VALIDATE_ACTION);
//        validationButton.addActionListener(this);
//        this.add(validationButton, BorderLayout.PAGE_END);
//        validationButton.setEnabled(isFastValidationSupport());
//        buttonsPanel.add(validationButton);

        this.add(buttonsPanel, BorderLayout.EAST);
        // repaint
        this.revalidate();
        this.repaint();
    }

    /**
     * Create a JLabel to represent a file count
     *
     * @param message file type message
     * @param count   file count
     * @return a label represents the file count
     */
    private JLabel createCountLabel(String message, int count) {
        JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));

        // set icon
        Icon icon;
        if (count <= 0) {
            icon = GUIUtilities.loadIcon(appContext.getProperty("file.zero.count.small.icon"));
        } else {
            icon = GUIUtilities.loadIcon(appContext.getProperty("file.non.zero.count.small.icon"));
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
        label.setIcon(icon);

        // set message
        label.setText(message + ": " + count);

        return label;
    }

    /**
     * Count different type of submission files
     *
     * @return an array of integer represents the count
     */
    private synchronized int[] countSubmissionFiles() {
        Submission submission = appContext.getSubmissionRecord().getSubmission();

        int[] count = new int[6];
        java.util.List<DataFile> dataFiles = submission.getDataFiles();
        count[0] = dataFiles.size();
        for (DataFile dataFile : dataFiles) {
            switch (dataFile.getFileType()) {
                case RESULT:
                    count[1]++;
                    break;
                case RAW:
                    count[2]++;
                    break;
                case PEAK:
                    count[3]++;
                    break;
                case SEARCH:
                    count[4]++;
                    break;
                // Easy change for compacting code
                case GEL:
                    // count[5]++;
                    // break;
                case QUANT:
                    // count[5]++;
                    //  break;
                case OTHER:
                    count[5]++;
                    break;
            }
        }
        return count;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (AppContext.ADD_NEW_DATA_FILE.equals(propName)
                || AppContext.REMOVE_DATA_FILE.equals(propName)
                || AppContext.CHANGE_DATA_FILE_TYPE.equals(propName)
//                || AppContext.ADD_NEW_DATA_FILE_MAPPING.equals(propName)
//                || AppContext.REMOVE_DATA_FILE_MAPPING.equals(propName)
                || AppContext.NEW_SUBMISSION_FILE.equals(propName)) {
            populateSummaryItemPanel();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (EXPORT_SUMMARY_ACTION.equals(e.getActionCommand())) {
                SummaryDescriptor.exportSummary(appContext);
            }
            if (VALIDATE_ACTION.equals(e.getActionCommand())) {
                runBackgroundValidation();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//    /**
//     * This method validate the results files before they get submitted. It construct the commands to
//     * call ms-data-core-api to validate results files along with peak files
//     */
//    private void validateFiles() {
//        String validationHTMLReport = "";
//        List<String[]> validationCommands = new ArrayList<>();
//        List<DataFile> dataFiles = submission.getDataFiles();
//        dataFiles = filterByFileScanDepth(dataFiles);
//
//        for (DataFile dataFile : dataFiles) {
//            if (dataFile.getFileType().equals(ProjectFileType.RESULT)) {
//                validationCommands.add(constructValidationCommand(dataFile, dataFile.getFileFormat()));
//                validationHTMLReport += "\n\n";
//            }
//        }
//        List<Report> reports = runValidationCommands(validationCommands);
//        ValidationReportHTMLFormatUtil formatUtil = new ValidationReportHTMLFormatUtil();
//        validationHTMLReport = formatUtil.getValidationReportInHTML(submission, reports).toString();
//        ValidationReportFrame validationReport = new ValidationReportFrame(validationHTMLReport);
//        validationReport.open();
//    }

    private List<DataFile> filterByFileScanDepth(List<DataFile> dataFiles) {
        List<DataFile> filteredDataFiles = new ArrayList<>();

        String scanDepth = SINGLE_RESULT_FILE;
        logger.info("Found validation.file.scan.depth: " + scanDepth);
        switch (scanDepth) {
            case SINGLE_RESULT_FILE:
                dataFiles = dataFiles.stream().filter(dataFile ->
                        dataFile.isFile() &&
                                dataFile.getFileFormat() == MassSpecFileFormat.MZIDENTML ||
                                dataFile.getFileFormat() == MassSpecFileFormat.MZTAB ||
                                dataFile.getFileFormat() == MassSpecFileFormat.PRIDE).collect(Collectors.toList());
                // assume first one is the smallest
                DataFile smallestDataFile = dataFiles.get(0);
                for (DataFile dataFile : dataFiles) {
                    if (dataFile.getFile().length() < smallestDataFile.getFile().length()) {
                        smallestDataFile = dataFile;
                    }
                }
                filteredDataFiles.add(smallestDataFile);
                break;
            case MULTIPLE_RESULT_FILES:
                filteredDataFiles = dataFiles;
                break;
            default:
                try {
                    throw new Exception(
                            "Invalid setting found for validation.file.scan.depth which expect either \"single\" or \"multiple\", but found:" + scanDepth);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return filteredDataFiles;
    }

    /**
     * Fast Validation supports to only submissions that are:
     * 1) Complete Submissions
     * 2) MzIdentML formats only
     *
     * @return True if both conditions get satisfied
     */
    private boolean isFastValidationSupport() {
        boolean isQualified = false;
        try {
            if (submission.getProjectMetaData().getSubmissionType().equals(SubmissionTypeConstants.COMPLETE)) {
                for (DataFile dataFile : submission.getDataFiles()) {
                    if (dataFile.getFileFormat() != null && dataFile.getFileFormat().equals(MassSpecFileFormat.MZIDENTML)) {
                        isQualified = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isQualified;
    }

//    /**
//     * This method is a command method to construct mzIdentML, MzTab or PRIDEXML validations using
//     * validation tools on ms-data-core-api
//     *
//     * @param dataFile
//     * @param fileFormat
//     * @return
//     */
//    private String[] constructValidationCommand(DataFile dataFile, MassSpecFileFormat fileFormat) {
//        File reportFile;
//        boolean isFirstPeakFile = true;
//        List<String> command = new ArrayList<>();
//        String arg_format =
//                (!fileFormat.equals(MassSpecFileFormat.PRIDE)) ? fileFormat.getFileExtension() : "pridexml";
//
//        try {
//            reportFile = File.createTempFile("testResultFile", ".log");
//            command.add("-" + ARG_VALIDATION);
//            command.add("-" + arg_format);
//            command.add(dataFile.getFilePath());
//            if (!fileFormat.equals(MassSpecFileFormat.PRIDE)) {

    /// /                for (DataFile mappingFile : dataFile.getFileMappings()) {
    /// /                    if (mappingFile.getFileType().equals(ProjectFileType.PEAK)) {
    /// /                        command.add(isFirstPeakFile ? "-" + ARG_PEAK : "##");
    /// /                        command.add(mappingFile.getFilePath());
    /// /                        isFirstPeakFile = false;
    /// /                    }
    /// /                }
//            }
//            if (fileFormat.equals(MassSpecFileFormat.MZIDENTML) && IS_FAST_VALIDATION_ENABLED) {
//                logger.debug("Fast Validation switched on");
//                command.add("-" + ARG_FAST_VALIDATION);
//            }
//            command.add("-" + ARG_SCHEMA_VALIDATION);
//            command.add("-" + ARG_SKIP_SERIALIZATION);
//            command.add("-" + ARG_REPORTFILE);
//            command.add(reportFile.getAbsolutePath());
//        } catch (IOException e) {
//            logger.error(
//                    "Error occurred in construction of MzIdentML validation command: " + e.getMessage());
//        }
//        return command.stream().toArray(String[]::new);
//    }
//
//    /**
//     * This method executes the validation commands
//     *
//     * @param validationCommands List of arrays of String
//     * @return String
//     */
//    private List<Report> runValidationCommands(List<String[]> validationCommands) {
//        List<Report> reports = new ArrayList<>();
//        try {
//            for (String[] args : validationCommands) {
//                CommandLine cmd = MainApp.parseArgs(args);
//                logger.debug("Running command: " + Arrays.toString(args));
//                reports.add(Validator.startValidation(cmd));
//            }
//        } catch (ParseException e) {
//            logger.error("File validation error: " + e.getMessage());
//        }
//        return reports;
//    }
    private int runBackgroundValidation() {

        ValidationProgressDialog validationProgessJframe =
                new ValidationProgressDialog(null, "title", "message");
        SwingWorker<Boolean, String> worker =
                new SwingWorker<Boolean, String>() {

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        //validateFiles();
                        return true;
                    }

                    @Override
                    protected void process(List<String> chunks) {
                        String value = chunks.get(chunks.size() - 1);
                    }

                    @Override
                    protected void done() {
                        if (isCancelled()) {
                            return;
                        }
                        try {
                            // Retrieve the return value of doInBackground.
                            boolean status = get();
                            logger.info("Validation completed with the status: " + status);
                            validationProgessJframe.dispose(); // close the dialog
                        } catch (InterruptedException e) {
                            // This is thrown if the thread's interrupted.
                            logger.error("Error:" + e);
                        } catch (ExecutionException e) {
                            // This is thrown if we throw an exception
                            // from doInBackground.
                            logger.error("Error:" + e);
                        }
                    }
                };

        worker.execute(); // this will start the processing on a separate thread
        validationProgessJframe.setVisible(true);
        worker.cancel(validationProgessJframe.shouldCancel); // stop further processing
        return 0;
    }

    private void addChecksumFile() {
        DataFile checksumDataFile = new DataFile();
        DesktopContext context = App.getInstance().getDesktopContext();
        
        if (!checksumDataFile.isFile()) {
            try {
                // Ensure checksum file is created in current directory
                logger.info("Creating new checksum file in current directory: {}", checksumFile.getAbsolutePath());
                
                // Create the checksum file
                if (!checksumFile.createNewFile()) {
                    throw new IOException("Failed to create checksum file: " + checksumFile.getAbsolutePath());
                }
                
                // Write the initial header
                Files.write("#Checksum File\n".getBytes(), checksumFile);
                logger.info("Successfully created checksum file: {}", checksumFile.getAbsolutePath());
                
                // Set up the data file
                checksumDataFile.setFile(checksumFile);
                checksumDataFile.setFileType(ProjectFileType.OTHER);
                
                // Add to submission if not already present
                String checksumFilename = context.getProperty("checksum.filename");
                if (((AppContext) context).getSubmissionRecord().getSubmission().getDataFiles().stream()
                    .noneMatch(dataFile -> dataFile.getFileName().equals(checksumFilename))) {
                    ((AppContext) context).addDataFile(checksumDataFile);
                    logger.info("Added checksum file to submission: {}", checksumFilename);
                } else {
                    logger.debug("Checksum file already present in submission: {}", checksumFilename);
                }
                
            } catch (Exception ex) {
                logger.error("Failed to create checksum file: {}", checksumFile.getAbsolutePath(), ex);
                
                // Show detailed error message
                String errorMessage = "Failed to create checksum file in current directory.\n\n" +
                    "File: " + checksumFile.getAbsolutePath() + "\n" +
                    "Error: " + ex.getMessage() + "\n\n" +
                    "Please ensure you have write permissions in the current directory.";
                    
                JOptionPane.showMessageDialog(app.getMainFrame(),
                    errorMessage,
                    "Checksum File Creation Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
