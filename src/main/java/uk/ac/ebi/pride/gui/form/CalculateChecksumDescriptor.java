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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalculateChecksumDescriptor extends ContextAwareNavigationPanelDescriptor implements TaskListener<DataFile, ChecksumMessage> {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionTypeDescriptor.class);
    private static final String PRIDE_CHECKSUM_GUIDE_URL = "https://www.ebi.ac.uk/pride/markdownpage/checksum";

    public static Map<String, Tuple<String, String>> checksumCalculatedFiles = new HashMap<>();

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
            calculateChecksumForm.setProgressMessage("Provided checksum.txt will be validated when you click Next.");
            nextButton.setEnabled(true);
        } else {
            File checksumFileRef = checksumDataFile != null ? checksumDataFile.getFile() : null;
            ChecksumValidationResult existingValidation = null;
            try {
                existingValidation = validateChecksumFile(dataFiles, checksumFileRef);
            } catch (IOException e) {
                logger.warn("Could not validate existing checksum.txt", e);
            }

            // Tool-generated or existing checksum.txt that already covers all files: no provide/calculate popup.
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
                            : existingValidation.getMissingFileNames())) {
                try {
                    ensureChecksumFile(dataFiles);
                    startChecksumCalculation();
                } catch (IOException e) {
                    logger.error("Error preparing checksum file", e);
                    JOptionPane.showMessageDialog(
                            app.getMainFrame(),
                            "Could not prepare checksum.txt for checksum calculation.\n\nError: " + e.getMessage(),
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
            if (file.getFileName().equals("checksum.txt"))
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
                } else if (confirmChecksumChoice(validationResult.getChecksumFile(), validationResult.getMissingFileNames())) {
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
        String checksumFilename = appContext.getProperty("checksum.filename");
        for (DataFile dataFile : dataFiles) {
            if (checksumFilename.equals(dataFile.getFileName())) {
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
        File defaultChecksumFile = new File(appContext.getProperty("checksum.filename"));
        return file != null && !file.getAbsolutePath().equals(defaultChecksumFile.getAbsolutePath());
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
                : new File(appContext.getProperty("checksum.filename"));
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
        List<String> missingFileNames = new ArrayList<>();
        if (checksumFile == null || !checksumFile.exists() || !checksumFile.canRead()) {
            for (DataFile dataFile : dataFiles) {
                if (!isChecksumDataFile(dataFile)) {
                    missingFileNames.add(dataFile.getFileName());
                }
            }
            return new ChecksumValidationResult(checksumFile, missingFileNames);
        }

        Set<String> checksumEntries = readChecksumEntries(checksumFile);
        for (DataFile dataFile : dataFiles) {
            if (!isChecksumDataFile(dataFile) && !checksumEntries.contains(dataFile.getFileName())) {
                missingFileNames.add(dataFile.getFileName());
            }
        }
        return new ChecksumValidationResult(checksumFile, missingFileNames);
    }

    private Set<String> readChecksumEntries(File checksumFile) throws IOException {
        Set<String> checksumEntries = new HashSet<>();
        List<String> lines = java.nio.file.Files.readAllLines(checksumFile.toPath(), Charset.defaultCharset());
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            String[] parts = trimmedLine.split("\\s+");
            if (parts.length > 0) {
                checksumEntries.add(parts[0]);
                checksumEntries.add(new File(parts[0]).getName());
            }
        }
        return checksumEntries;
    }

    private boolean isChecksumDataFile(DataFile dataFile) {
        return appContext.getProperty("checksum.filename").equals(dataFile.getFileName());
    }

    private boolean confirmChecksumChoice(File checksumFile, List<String> missingFileNames) {
        StringBuilder message = new StringBuilder();
        if (missingFileNames == null) {
            message.append("Please provide the checksum file for this submission.");
        } else {
            message.append("The provided checksum.txt does not include all selected files.");
            if (checksumFile != null) {
                message.append("\n\nChecksum file:\n").append(checksumFile.getAbsolutePath());
            }
            message.append("\n\nMissing file names:");
            int limit = Math.min(missingFileNames.size(), 20);
            for (int i = 0; i < limit; i++) {
                message.append("\n- ").append(missingFileNames.get(i));
            }
            if (missingFileNames.size() > limit) {
                message.append("\n... and ").append(missingFileNames.size() - limit).append(" more");
            }
        }

        message.append("\n\nYou can create checksum.txt using this PRIDE checksum guide:\n")
                .append(PRIDE_CHECKSUM_GUIDE_URL)
                .append("\n\nIf you choose to calculate checksums in the submission tool, it can take a long time for large submissions.");

        Object[] options = {"Please provide checksum", "Calculate checksum"};
        int option = JOptionPane.showOptionDialog(
                app.getMainFrame(),
                createWrappedMessage(message.toString()),
                "Checksum file required",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        return option == JOptionPane.NO_OPTION;
    }

    private JTextArea createWrappedMessage(String text) {
        JTextArea messageArea = new JTextArea(text);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setColumns(72);
        return messageArea;
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
        logger.info("Writing calculated checksum to checksum.txt");
        Files.write("#Checksum File\n".getBytes(), SummaryItemPanel.checksumFile);
        int countOfChecksumCalculatedFiles = 0;
        for (DataFile dataFile : dataFiles) {
            try {
                if (checksumCalculatedFiles.containsKey(dataFile.getFilePath()) &&
                        !dataFile.getFile().getName().equals("checksum.txt")) {
                    Files.append(dataFile.getFilePath() + "\t" +
                                    checksumCalculatedFiles.get(dataFile.getFilePath()).getValue() + "\n",
                            SummaryItemPanel.checksumFile, Charset.defaultCharset());
                    countOfChecksumCalculatedFiles++;
                } else if (!dataFile.getFile().getName().equals("checksum.txt")) {
                    return false;
                }
            } catch (Exception ex) {
                logger.error("Error in adding file " + dataFile.getFile().getName() + " to checksum.txt");
                logger.error(ex.getMessage());
                return false;
            }
        }
        logger.info("Checksum calculated and written to checksum.txt for all files");
        logger.info("Files count " + countOfChecksumCalculatedFiles);
        return true;
    }

    private static class ChecksumValidationResult {
        private final File checksumFile;
        private final List<String> missingFileNames;

        private ChecksumValidationResult(File checksumFile, List<String> missingFileNames) {
            this.checksumFile = checksumFile;
            this.missingFileNames = missingFileNames;
        }

        private boolean isValid() {
            return missingFileNames.isEmpty();
        }

        private File getChecksumFile() {
            return checksumFile;
        }

        private List<String> getMissingFileNames() {
            return missingFileNames;
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
