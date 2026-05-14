package uk.ac.ebi.pride.gui.form;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.form.panel.SummaryItemPanel;
import uk.ac.ebi.pride.gui.navigation.Navigator;
import uk.ac.ebi.pride.gui.util.ChecksumSubmissionValidator;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.task.CalculateChecksumTask;
import uk.ac.ebi.pride.gui.task.checksum.ChecksumMessage;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListener;
import uk.ac.ebi.pride.utilities.util.Tuple;

import javax.help.HelpBroker;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateChecksumDescriptor extends ContextAwareNavigationPanelDescriptor implements TaskListener<DataFile, ChecksumMessage> {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionTypeDescriptor.class);
    private static final String PRIDE_CHECKSUM_GUIDE_URL = "https://www.ebi.ac.uk/pride/markdownpage/checksum";

    public static Map<String, Tuple<String, String>> checksumCalculatedFiles = new HashMap<>();

    public static void resetChecksumCalculationCache() {
        checksumCalculatedFiles.clear();
    }

    public boolean dontCalculateChecksum = true;
    private boolean usingProvidedChecksumFile;
    private boolean checksumCalculationStarted;

    public CalculateChecksumDescriptor(String id, String title, String desc) {
        super(id, title, desc, new CalculateChecksumForm());
    }

    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.checksum.type", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public boolean toSkipPanel() {
        return appContext.isCustomChecksumFileProvided();
    }

    @Override
    public void displayingPanel() {
        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();

        nextButton.setText(appContext.getProperty("checksum.submit.button.title"));
        nextButton.setEnabled(false);

        CalculateChecksumForm calculateChecksumForm = (CalculateChecksumForm) CalculateChecksumDescriptor.this.getNavigationPanel();
        calculateChecksumForm.enableCancelButton(true);
        calculateChecksumForm.setProgressMessage(appContext.getProperty("checksum.default.message"));

        usingProvidedChecksumFile = false;
        checksumCalculationStarted = false;
        List<DataFile> dataFiles = appContext.getSubmissionRecord().getSubmission().getDataFiles();
        DataFile checksumDataFile = getChecksumDataFile(dataFiles);

        if (isCustomChecksumFile(checksumDataFile)) {
            SummaryItemPanel.checksumFile = checksumDataFile.getFile();
            usingProvidedChecksumFile = true;
            dontCalculateChecksum = true;
            calculateChecksumForm.enableCancelButton(false);
            calculateChecksumForm.setProgressMessage(
                    "Provided " + Constant.CHECKSUM_FILE_NAME + " will be validated when you click Next.");
            nextButton.setEnabled(true);
        } else {
            File checksumFileRef = checksumDataFile != null ? checksumDataFile.getFile() : null;
            ChecksumValidationResult existingValidation = null;
            try {
                existingValidation = validateChecksumFile(dataFiles, checksumFileRef);
            } catch (IOException e) {
                logger.warn("Could not validate existing {}", Constant.CHECKSUM_FILE_NAME, e);
            }

            // Tool-generated or existing checksum file that already covers all files: no provide/calculate popup.
            if (checksumDataFile != null && existingValidation != null && existingValidation.isValid()) {
                SummaryItemPanel.checksumFile = checksumFileRef;
                dontCalculateChecksum = true;
                usingProvidedChecksumFile = false;
                calculateChecksumForm.enableCancelButton(false);
                calculateChecksumForm.setProgressMessage(appContext.getProperty("checksum.complete.ready.message"));
                nextButton.setEnabled(true);
            } else if (confirmChecksumChoice(checksumFileRef,
                    existingValidation == null || existingValidation.isValid()
                            ? null
                            : existingValidation)) {
                try {
                    ensureChecksumFile(dataFiles);
                    startChecksumCalculation();
                } catch (IOException e) {
                    logger.error("Error preparing checksum file", e);
                    JOptionPane.showMessageDialog(
                            app.getMainFrame(),
                            "Could not prepare " + Constant.CHECKSUM_FILE_NAME + " for checksum calculation.\n\nError: " + e.getMessage(),
                            "Checksum File Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                navigateBackToFileSelectionPage();
            }
        }
        firePropertyChange(DISPLAYING_PANEL_PROPERTY, false, true);
    }

    private void removeChecksumFile() {
        List<DataFile> files = ((AppContext) App.getInstance().getDesktopContext()).getSubmissionRecord().getSubmission().getDataFiles();
        for (DataFile file : files) {
            if (file.getFileName().equals(Constant.CHECKSUM_FILE_NAME))
                ((AppContext) App.getInstance().getDesktopContext()).removeDatafile(file);
        }
    }

    @Override
    public void beforeHidingForNextPanel() {
        try {
            if (usingProvidedChecksumFile) {
                ChecksumValidationResult validationResult =
                        validateChecksumFile(appContext.getSubmissionRecord().getSubmission().getDataFiles(), SummaryItemPanel.checksumFile);
                if (validationResult.isValid()) {
                    firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
                } else if (confirmChecksumChoice(validationResult.getChecksumFile(), validationResult)) {
                    usingProvidedChecksumFile = false;
                    ensureChecksumFile(appContext.getSubmissionRecord().getSubmission().getDataFiles());
                    startChecksumCalculation();
                    firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
                } else {
                    navigateBackToFileSelectionPage();
                    firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
                }
                return;
            }

            if (dontCalculateChecksum || checkAndWriteChecksum(appContext.getSubmissionRecord().getSubmission().getDataFiles())) {
                firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
            } else {
                firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
            }
        } catch (Exception ex) {
            logger.error("Error in writing checksum file", ex);
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
        }
    }

    private void startChecksumCalculation() {
        if (checksumCalculationStarted) {
            return;
        }

        dontCalculateChecksum = false;
        checksumCalculationStarted = true;
        CalculateChecksumForm calculateChecksumForm = (CalculateChecksumForm) CalculateChecksumDescriptor.this.getNavigationPanel();
        calculateChecksumForm.enableCancelButton(true);
        calculateChecksumForm.setProgressMessage(appContext.getProperty("checksum.default.message"));

        Navigator navigator = ((App) App.getInstance()).getNavigator();
        navigator.getNextButton().setEnabled(false);

        Task newTask = new CalculateChecksumTask(appContext.getSubmissionRecord().getSubmission());
        newTask.addTaskListener(this);
        newTask.setGUIBlocker(new DefaultGUIBlocker(newTask, GUIBlocker.Scope.NONE, null));
        appContext.addTask(newTask);
    }

    private DataFile getChecksumDataFile(List<DataFile> dataFiles) {
        for (DataFile dataFile : dataFiles) {
            if (Constant.CHECKSUM_FILE_NAME.equals(dataFile.getFileName())) {
                return dataFile;
            }
        }
        return null;
    }

    private boolean isCustomChecksumFile(DataFile checksumDataFile) {
        if (checksumDataFile == null) {
            return false;
        }
        if (((AppContext) App.getInstance().getDesktopContext()).isCustomChecksumFileProvided()) {
            return true;
        }
        File file = checksumDataFile.getFile();
        if (file == null) {
            return false;
        }
        File defaultChecksumFile = resolveDefaultChecksumLocation(file);
        try {
            return !file.getCanonicalFile().equals(defaultChecksumFile.getCanonicalFile());
        } catch (IOException e) {
            logger.warn("Could not resolve checksum file paths for default vs custom detection; using absolute path comparison", e);
            return !file.getAbsoluteFile().equals(defaultChecksumFile.getAbsoluteFile());
        }
    }

    /**
     * Resolves {@code checksum.filename} from configuration: absolute paths are used as-is;
     * relative names are resolved next to the directory containing the current checksum file (not JVM CWD).
     */
    private File resolveDefaultChecksumLocation(File checksumFile) {
        String configured = appContext.getProperty("checksum.filename");
        if (configured == null || configured.isEmpty()) {
            configured = Constant.CHECKSUM_FILE_NAME;
        }
        File asDeclared = new File(configured);
        if (asDeclared.isAbsolute()) {
            return asDeclared;
        }
        File parent = checksumFile.getAbsoluteFile().getParentFile();
        return parent != null ? new File(parent, asDeclared.getPath()) : new File(asDeclared.getPath());
    }

    private void ensureChecksumFile(List<DataFile> dataFiles) throws IOException {
        DataFile checksumDataFile = getChecksumDataFile(dataFiles);
        if (checksumDataFile != null) {
            SummaryItemPanel.checksumFile = checksumDataFile.getFile();
            ((AppContext) App.getInstance().getDesktopContext()).setCustomChecksumFileProvided(false);
            return;
        }

        File checksumFile = SummaryItemPanel.checksumFile != null
                ? SummaryItemPanel.checksumFile
                : new File(Constant.CHECKSUM_FILE_NAME);
        if (!checksumFile.exists()) {
            java.nio.file.Files.write(checksumFile.toPath(), "#Checksum File\n".getBytes(Charset.defaultCharset()));
        }

        DataFile newChecksumDataFile = new DataFile();
        newChecksumDataFile.setFile(checksumFile);
        newChecksumDataFile.setFileType(ProjectFileType.OTHER);
        ((AppContext) App.getInstance().getDesktopContext()).addDataFile(newChecksumDataFile);
        ((AppContext) App.getInstance().getDesktopContext()).setCustomChecksumFileProvided(false);
        SummaryItemPanel.checksumFile = checksumFile;
    }

    private ChecksumValidationResult validateChecksumFile(List<DataFile> dataFiles, File checksumFile) throws IOException {
        ChecksumSubmissionValidator.Result r =
                ChecksumSubmissionValidator.validate(dataFiles, checksumFile, Constant.CHECKSUM_FILE_NAME);
        return new ChecksumValidationResult(checksumFile,
                new ArrayList<>(r.getMissingInChecksum()),
                new ArrayList<>(r.getExtraInChecksum()));
    }

    private boolean isChecksumDataFile(DataFile dataFile) {
        return Constant.CHECKSUM_FILE_NAME.equals(dataFile.getFileName());
    }

    private boolean confirmChecksumChoice(File checksumFile, ChecksumValidationResult invalidDetailOrNull) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"width:420px;font-family:sans-serif;font-size:11pt;\">");
        if (invalidDetailOrNull == null) {
            html.append("<p>").append(htmlEscape("Please provide the checksum file for this submission.")).append("</p>");
        } else {
            html.append("<p>").append(htmlEscape("The provided " + Constant.CHECKSUM_FILE_NAME + " is not valid for the selected files.")).append("</p>");
            if (checksumFile != null) {
                html.append("<p><b>").append(htmlEscape("Checksum file:")).append("</b><br>")
                        .append(htmlEscape(checksumFile.getAbsolutePath())).append("</p>");
            }
            if (!invalidDetailOrNull.getMissingFileNames().isEmpty()) {
                html.append("<p><b>").append(htmlEscape("Selected files not listed in " + Constant.CHECKSUM_FILE_NAME + ":")).append("</b></p><ul>");
                int limit = Math.min(invalidDetailOrNull.getMissingFileNames().size(), 20);
                for (int i = 0; i < limit; i++) {
                    html.append("<li>").append(htmlEscape(invalidDetailOrNull.getMissingFileNames().get(i))).append("</li>");
                }
                html.append("</ul>");
                if (invalidDetailOrNull.getMissingFileNames().size() > limit) {
                    html.append("<p>").append(htmlEscape("... and " + (invalidDetailOrNull.getMissingFileNames().size() - limit) + " more")).append("</p>");
                }
            }
            if (!invalidDetailOrNull.getExtraInChecksum().isEmpty()) {
                html.append("<p><b>").append(htmlEscape("Entries in " + Constant.CHECKSUM_FILE_NAME + " that do not match any selected file:")).append("</b></p><ul>");
                int limitEx = Math.min(invalidDetailOrNull.getExtraInChecksum().size(), 20);
                for (int i = 0; i < limitEx; i++) {
                    html.append("<li>").append(htmlEscape(invalidDetailOrNull.getExtraInChecksum().get(i))).append("</li>");
                }
                html.append("</ul>");
                if (invalidDetailOrNull.getExtraInChecksum().size() > limitEx) {
                    html.append("<p>").append(htmlEscape("... and " + (invalidDetailOrNull.getExtraInChecksum().size() - limitEx) + " more")).append("</p>");
                }
            }
        }
        html.append("<p>")
                .append(htmlEscape("You can create " + Constant.CHECKSUM_FILE_NAME + " using this "))
                .append("<a href=\"").append(PRIDE_CHECKSUM_GUIDE_URL).append("\">")
                .append(htmlEscape("PRIDE checksum guide")).append("</a>")
                .append(htmlEscape(".")).append("</p>");
        html.append("<p>")
                .append(htmlEscape("If you choose to calculate checksums in the submission tool, it can take a long time for large submissions."))
                .append("</p>");
        html.append("</body></html>");

        Object[] options = {"Please provide checksum", "Calculate checksum"};
        int option = JOptionPane.showOptionDialog(
                app.getMainFrame(),
                createClickableHtmlMessage(html.toString()),
                "Checksum file required",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        return option == JOptionPane.NO_OPTION;
    }

    private static String htmlEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private JEditorPane createClickableHtmlMessage(String html) {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html; charset=UTF-8");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setText(html);
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } else {
                    JOptionPane.showMessageDialog(app.getMainFrame(),
                            "Please open this link in a browser:\n" + e.getURL(),
                            "Open link",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                logger.warn("Could not open hyperlink", ex);
                JOptionPane.showMessageDialog(app.getMainFrame(),
                        "Could not open the link. Please open it manually:\n" + e.getURL(),
                        "Link error",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
        pane.setPreferredSize(new Dimension(460, 280));
        return pane;
    }

    private void navigateBackToFileSelectionPage() {
        SwingUtilities.invokeLater(() -> {
            Navigator navigator = ((App) App.getInstance()).getNavigator();
            if (navigator.getNavigationModel().getBackPanelDescriptor() != null) {
                navigator.setCurrentNavigationPanel(
                        navigator.getNavigationModel().getBackPanelDescriptor().getNavigationPanelId());
            }
        });
    }

    private boolean checkAndWriteChecksum(List<DataFile> dataFiles) throws Exception {
        logger.info("Writing calculated checksum to {}", Constant.CHECKSUM_FILE_NAME);
        Files.write("#Checksum File\n".getBytes(), SummaryItemPanel.checksumFile);
        int countOfChecksumCalculatedFiles = 0;
        for (DataFile dataFile : dataFiles) {
            try {
                if (checksumCalculatedFiles.containsKey(dataFile.getFilePath()) &&
                        !dataFile.getFile().getName().equals(Constant.CHECKSUM_FILE_NAME)) {
                    Files.append(dataFile.getFilePath() + "\t" +
                                    checksumCalculatedFiles.get(dataFile.getFilePath()).getValue() + "\n",
                            SummaryItemPanel.checksumFile, Charset.defaultCharset());
                    countOfChecksumCalculatedFiles++;
                } else if (!dataFile.getFile().getName().equals(Constant.CHECKSUM_FILE_NAME)) {
                    return false;
                }
            } catch (Exception ex) {
                logger.error("Error in adding file {} to {}", dataFile.getFile().getName(), Constant.CHECKSUM_FILE_NAME);
                logger.error(ex.getMessage());
                return false;
            }
        }
        logger.info("Checksum calculated and written to {} for all files", Constant.CHECKSUM_FILE_NAME);
        logger.info("Files count " + countOfChecksumCalculatedFiles);
        return true;
    }

    private static class ChecksumValidationResult {
        private final File checksumFile;
        private final List<String> missingFileNames;
        private final List<String> extraInChecksum;

        private ChecksumValidationResult(File checksumFile, List<String> missingFileNames, List<String> extraInChecksum) {
            this.checksumFile = checksumFile;
            this.missingFileNames = missingFileNames;
            this.extraInChecksum = extraInChecksum;
        }

        private boolean isValid() {
            return missingFileNames.isEmpty() && extraInChecksum.isEmpty();
        }

        private File getChecksumFile() {
            return checksumFile;
        }

        private List<String> getMissingFileNames() {
            return missingFileNames;
        }

        private List<String> getExtraInChecksum() {
            return extraInChecksum;
        }
    }


    @Override
    public void started(TaskEvent<Void> taskEvent) {

    }

    @Override
    public void process(TaskEvent<List<ChecksumMessage>> taskEvent) {
        List<ChecksumMessage> checksumMessages = taskEvent.getValue();
        for (ChecksumMessage checksumMessage : checksumMessages) {
            try {
                CalculateChecksumForm calculateChecksumForm = (CalculateChecksumForm) CalculateChecksumDescriptor.this.getNavigationPanel();
                calculateChecksumForm.setProgress(checksumMessage.getNoOfFilesProcessed(), checksumMessage.getTotalNoOfFiles());
                Tuple<String, String> fileChecksum = checksumMessage.getFileChecksum();
                if(fileChecksum!=null) {
                    String filePath = fileChecksum.getKey();
                    if (!checksumCalculatedFiles.containsKey(filePath)) {
                        checksumCalculatedFiles.put(filePath, fileChecksum);
                    }
                }
            } catch (Exception exception) {
                JOptionPane.showConfirmDialog(app.getMainFrame(),
                        appContext.getProperty("checksum.error.message"),
                        appContext.getProperty("checksum.error.title"),
                        JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    @Override
    public void succeed(TaskEvent<DataFile> taskEvent) {
    }

    @Override
    public void finished(TaskEvent<Void> event) {
    }

    @Override
    public void failed(TaskEvent<Throwable> event) {
        JOptionPane.showConfirmDialog(app.getMainFrame(),
                appContext.getProperty("checksum.file.selection.error.message"),
                appContext.getProperty("checksum.file.error.title"),
                JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void cancelled(TaskEvent<Void> event) {
    }

    @Override
    public void interrupted(TaskEvent<InterruptedException> iex) {
    }

    @Override
    public void progress(TaskEvent<Integer> progress) {
    }
}
