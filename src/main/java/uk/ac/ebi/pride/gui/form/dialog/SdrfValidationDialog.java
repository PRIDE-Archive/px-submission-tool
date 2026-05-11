package uk.ac.ebi.pride.gui.form.dialog;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareDialog;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;
import uk.ac.ebi.pride.gui.navigation.NavigationControlPanel;
import uk.ac.ebi.pride.gui.util.SdrfValidatorClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SdrfValidationDialog extends ContextAwareDialog {
    private final DataFile dataFile;
    private final SdrfValidatorClient validatorClient;
    private final JComboBox<SdrfValidatorClient.TemplateInfo> templateComboBox;
    private final JCheckBox skipOntologyCheckBox;
    private final JCheckBox useOlsCacheOnlyCheckBox;
    private final JButton validateButton;
    private final JButton closeButton;
    private final JTextArea resultTextArea;
    private final JLabel statusLabel;

    private SdrfValidatorClient.ValidationResult validationResult;
    private boolean cancelled = true;

    public static SdrfValidatorClient.ValidationResult showDialog(Frame owner, DataFile dataFile)
            throws Exception {
        SdrfValidatorClient validatorClient = new SdrfValidatorClient();
        AtomicReference<SdrfValidationDialog> dialogReference = new AtomicReference<>();

        runOnEventDispatchThread(() -> {
            SdrfValidationDialog dialog = new SdrfValidationDialog(owner, dataFile, validatorClient);
            dialog.setLocationRelativeTo(owner);
            dialogReference.set(dialog);
            dialog.setVisible(true);
        });

        SdrfValidationDialog dialog = dialogReference.get();
        if (dialog == null || dialog.cancelled) {
            return null;
        }
        return dialog.validationResult;
    }

    private static void runOnEventDispatchThread(Runnable runnable) throws InvocationTargetException, InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeAndWait(runnable);
        }
    }

    private SdrfValidationDialog(Frame owner,
                                 DataFile dataFile,
                                 SdrfValidatorClient validatorClient) {
        super(owner, "SDRF validation", true);
        this.dataFile = dataFile;
        this.validatorClient = validatorClient;
        this.templateComboBox = new JComboBox<>();
        this.skipOntologyCheckBox = new JCheckBox("Skip ontology term validation");
        this.useOlsCacheOnlyCheckBox = new JCheckBox("Use OLS cache only", true);
        this.validateButton = new JButton("Validate");
        this.closeButton = new JButton(appContext.getProperty("close.button.label"));
        this.resultTextArea = new JTextArea();
        this.statusLabel = new JLabel("Loading SDRF validation templates...");
        initComponents();
    }

    private void initComponents() {
        setSize(new Dimension(760, 560));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeIfAllowed();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                loadTemplates();
            }
        });

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(contentPanel);

        contentPanel.add(createDetailsPanel(), BorderLayout.NORTH);
        contentPanel.add(createResultPanel(), BorderLayout.CENTER);
        contentPanel.add(createControlPanel(), BorderLayout.SOUTH);

        validateButton.addActionListener(e -> validateSdrf());
        closeButton.addActionListener(e -> closeIfAllowed());
        setTemplateLoadingInProgress(true);
    }

    private JPanel createDetailsPanel() {
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("SDRF details"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addDetailsRow(detailsPanel, constraints, 0, "File:", dataFile.getFileName());
        File validationFile = getValidationFile();
        addDetailsRow(detailsPanel, constraints, 1, "Path:", validationFile.getAbsolutePath());
        addDetailsRow(detailsPanel, constraints, 2, "Size:", formatFileSize(validationFile));

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0;
        detailsPanel.add(new JLabel("Template:"), constraints);

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 1;
        detailsPanel.add(templateComboBox, constraints);

        constraints.gridx = 1;
        constraints.gridy = 4;
        detailsPanel.add(skipOntologyCheckBox, constraints);

        constraints.gridy = 5;
        detailsPanel.add(useOlsCacheOnlyCheckBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        detailsPanel.add(statusLabel, constraints);

        return detailsPanel;
    }

    private void addDetailsRow(JPanel detailsPanel, GridBagConstraints constraints, int row, String label, String value) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        detailsPanel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.weightx = 1;
        detailsPanel.add(new JLabel(value), constraints);
    }

    private JPanel createResultPanel() {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("Validation errors and warnings"));

        resultTextArea.setEditable(false);
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        resultTextArea.setText("Validation has not run yet.");
        resultPanel.add(new JScrollPane(resultTextArea), BorderLayout.CENTER);
        return resultPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new NavigationControlPanel();
        controlPanel.setLayout(new BorderLayout());

        JPanel ctrlPane = new NonOpaquePanel(new FlowLayout(FlowLayout.RIGHT));
        ctrlPane.add(validateButton);
        ctrlPane.add(closeButton);
        controlPanel.add(ctrlPane, BorderLayout.EAST);
        return controlPanel;
    }

    private void loadTemplates() {
        SwingWorker<List<SdrfValidatorClient.TemplateInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SdrfValidatorClient.TemplateInfo> doInBackground() throws Exception {
                return validatorClient.getTemplates();
            }

            @Override
            protected void done() {
                try {
                    setTemplates(get());
                    statusLabel.setText("Choose a validation template, then validate the SDRF file.");
                    resultTextArea.setText("Validation has not run yet.");
                } catch (Exception e) {
                    setTemplates(SdrfValidatorClient.getFallbackTemplates());
                    statusLabel.setText("Could not load templates from API. Using the bundled template list.");
                    resultTextArea.setText("Template loading failed:\n" + e.getMessage()
                            + "\n\nThe bundled template list is available for validation.");
                } finally {
                    setTemplateLoadingInProgress(false);
                }
            }
        };
        worker.execute();
    }

    private void setTemplates(List<SdrfValidatorClient.TemplateInfo> templates) {
        DefaultComboBoxModel<SdrfValidatorClient.TemplateInfo> model = new DefaultComboBoxModel<>();
        for (SdrfValidatorClient.TemplateInfo template : templates) {
            model.addElement(template);
        }
        templateComboBox.setModel(model);
        if (model.getSize() > 0) {
            templateComboBox.setSelectedIndex(0);
        }
    }

    private void validateSdrf() {
        SdrfValidatorClient.TemplateInfo selectedTemplate =
                (SdrfValidatorClient.TemplateInfo) templateComboBox.getSelectedItem();
        if (selectedTemplate == null) {
            JOptionPane.showMessageDialog(this, "Please select an SDRF validation template.", "SDRF validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setValidationInProgress(true);
        SwingWorker<SdrfValidatorClient.ValidationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SdrfValidatorClient.ValidationResult doInBackground() throws Exception {
                return validatorClient.validate(
                        getValidationFile(),
                        selectedTemplate.getName(),
                        skipOntologyCheckBox.isSelected(),
                        useOlsCacheOnlyCheckBox.isSelected());
            }

            @Override
            protected void done() {
                try {
                    validationResult = get();
                    cancelled = false;
                    resultTextArea.setText(formatValidationResult(validationResult));
                    resultTextArea.setCaretPosition(0);
                    statusLabel.setText(validationResult.isValid()
                            ? "SDRF validation completed successfully."
                            : "SDRF validation completed with errors.");
                } catch (Exception e) {
                    validationResult = buildFailedValidationResult(e);
                    cancelled = false;
                    resultTextArea.setText(formatValidationResult(validationResult));
                    resultTextArea.setCaretPosition(0);
                    statusLabel.setText("SDRF validation failed.");
                } finally {
                    setValidationInProgress(false);
                }
            }
        };
        worker.execute();
    }

    private void setTemplateLoadingInProgress(boolean inProgress) {
        validateButton.setEnabled(!inProgress);
        templateComboBox.setEnabled(!inProgress);
        skipOntologyCheckBox.setEnabled(!inProgress);
        useOlsCacheOnlyCheckBox.setEnabled(!inProgress);
        if (inProgress) {
            resultTextArea.setText("Loading templates from PRIDE SDRF Validator API...");
        }
    }

    private SdrfValidatorClient.ValidationResult buildFailedValidationResult(Exception exception) {
        SdrfValidatorClient.ValidationResult result = new SdrfValidatorClient.ValidationResult();
        SdrfValidatorClient.ValidationIssue issue = new SdrfValidatorClient.ValidationIssue();
        issue.setMsg("Unable to validate SDRF with PRIDE SDRF Validator API: " + exception.getMessage());
        result.setValid(false);
        result.setErrors(List.of(issue));
        result.setError_count(1);
        return result;
    }

    private void setValidationInProgress(boolean inProgress) {
        validateButton.setEnabled(!inProgress);
        templateComboBox.setEnabled(!inProgress);
        skipOntologyCheckBox.setEnabled(!inProgress);
        useOlsCacheOnlyCheckBox.setEnabled(!inProgress);
        closeButton.setEnabled(!inProgress);
        if (inProgress) {
            statusLabel.setText("Validating SDRF with PRIDE SDRF Validator API...");
            resultTextArea.setText("Validation is running. Please wait...");
        }
    }

    private String formatValidationResult(SdrfValidatorClient.ValidationResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Valid: ").append(result.isValid() ? "yes" : "no").append('\n');
        builder.append("Templates used: ").append(String.join(", ", result.getTemplates_used())).append('\n');
        if (result.getSdrf_pipelines_version() != null) {
            builder.append("sdrf-pipelines version: ").append(result.getSdrf_pipelines_version()).append('\n');
        }
        builder.append('\n');

        builder.append("Errors (").append(result.getError_count()).append(")\n");
        if (result.getErrors().isEmpty()) {
            builder.append(result.getError_count() > 0
                    ? "The API reported errors but did not return error message details.\n"
                    : "No errors reported.\n");
        } else {
            appendIssues(builder, result.getErrors());
        }
        builder.append('\n');

        builder.append("Warnings (").append(result.getWarning_count()).append(")\n");
        if (result.getWarnings().isEmpty()) {
            builder.append(result.getWarning_count() > 0
                    ? "The API reported warnings but did not return warning message details.\n"
                    : "No warnings reported.\n");
        } else {
            appendIssues(builder, result.getWarnings());
        }

        return builder.toString();
    }

    private void appendIssues(StringBuilder builder, List<SdrfValidatorClient.ValidationIssue> issues) {
        for (SdrfValidatorClient.ValidationIssue issue : issues) {
            builder.append("- ").append(issue.format()).append('\n');
        }
    }

    private String formatFileSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) {
            return bytes + " bytes";
        }
        long kilobytes = bytes / 1024;
        if (kilobytes < 1024) {
            return kilobytes + " KB";
        }
        return (kilobytes / 1024) + " MB";
    }

    private File getValidationFile() {
        if (dataFile.getFilePath() != null) {
            return Paths.get(dataFile.getFilePath().toString()).toFile();
        }
        return dataFile.getFile();
    }

    private void closeIfAllowed() {
        if (validationResult == null && !cancelled) {
            dispose();
            return;
        }
        if (validationResult == null) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "SDRF validation has not completed. Do you want to cancel it?",
                    "SDRF validation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }
}
