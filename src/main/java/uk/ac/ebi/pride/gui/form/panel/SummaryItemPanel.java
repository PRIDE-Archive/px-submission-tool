package uk.ac.ebi.pride.gui.form.panel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.gui.util.ValidationReportHTMLFormatUtil;
import uk.ac.ebi.pride.gui.util.ValidationResults;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.utilities.data.controller.tools.PGConverter;
import uk.ac.ebi.pride.utilities.data.controller.tools.Validator;
import uk.ac.ebi.pride.utilities.data.controller.tools.utils.Report;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.pride.utilities.data.controller.tools.utils.Utility.*;
import static uk.ac.ebi.pride.utilities.data.controller.tools.utils.Utility.ARG_REPORTFILE;

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

  Submission submission;
  SubmissionType submissionType;

  public SummaryItemPanel() {
    //get submission details
    submission = appContext.getSubmissionRecord().getSubmission();
    submissionType = submission.getProjectMetaData().getSubmissionType();

    appContext.addPropertyChangeListener(this);
    populateSummaryItemPanel();
  }

  /** Populate summary item panel */
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

    // add export summary button
    JButton exportSummaryButton =
        new JButton(appContext.getProperty("export.summary.button.label"));
    BalloonTipUtil.createBalloonTooltip(
        exportSummaryButton, appContext.getProperty("export.summary.button.tooltip"));
    exportSummaryButton.setActionCommand(EXPORT_SUMMARY_ACTION);
    exportSummaryButton.addActionListener(this);
    this.add(exportSummaryButton, BorderLayout.EAST);

    // add validation button
    JButton validationButton = new JButton(appContext.getProperty("summary.validate.button.title"));
    BalloonTipUtil.createBalloonTooltip(
        validationButton, appContext.getProperty("summary.validate.button.tooltip"));
    validationButton.setActionCommand(VALIDATE_ACTION);
    validationButton.addActionListener(this);
    this.add(validationButton, BorderLayout.PAGE_END);
    validationButton.setEnabled(submissionType.equals(SubmissionType.COMPLETE));

    // repaint
    this.revalidate();
    this.repaint();
  }

  /**
   * Create a JLabel to represent a file count
   *
   * @param message file type message
   * @param count file count
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
        || AppContext.ADD_NEW_DATA_FILE_MAPPING.equals(propName)
        || AppContext.REMOVE_DATA_FILE_MAPPING.equals(propName)
        || AppContext.NEW_SUBMISSION_FILE.equals(propName)) {
      populateSummaryItemPanel();
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      if (EXPORT_SUMMARY_ACTION.equals(e.getActionCommand())) {
        exportSummary();
      }
      if (VALIDATE_ACTION.equals(e.getActionCommand())) {
        validateFiles();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /** This method exports the px summary file */
  private void exportSummary() {
    // create file chooser
    JFileChooser fileChooser = new JFileChooser(appContext.getOpenFilePath());

    // set file chooser title
    fileChooser.setDialogTitle(appContext.getProperty("export.summary.dialog.title"));

    // set dialog type
    fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

    // set default selected file
    fileChooser.setSelectedFile(
        new File(
            appContext.getOpenFilePath()
                + System.getProperty("file.separator")
                + appContext.getProperty("export.summary.default.summary.file.name")));

    // set selection mode
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false);

    // show dialog
    int result = fileChooser.showDialog(((App) App.getInstance()).getMainFrame(), null);

    // check the selection results from open file dialog
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      try {
        if (!selectedFile.exists()) {
          boolean created = selectedFile.createNewFile();
          if (!created) {
            logger.error("Failed to create summary file: " + selectedFile.getAbsolutePath());
            JOptionPane.showMessageDialog(
                ((App) App.getInstance()).getMainFrame(),
                appContext.getProperty("export.summary.error.dialog.message"),
                appContext.getProperty("export.summary.error.dialog.title"),
                JOptionPane.ERROR_MESSAGE);
            return;
          }
        }
        Submission submission = appContext.getSubmissionRecord().getSubmission();
        SubmissionFileWriter.write(submission, selectedFile);
      } catch (Exception ex) {
        logger.error("Failed to export summary file: " + selectedFile.getAbsolutePath());
        JOptionPane.showMessageDialog(
            ((App) App.getInstance()).getMainFrame(),
            appContext.getProperty("export.summary.error.dialog.message"),
            appContext.getProperty("export.summary.error.dialog.title"),
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * This method validate the results files before they get submitted. It construct the commands to
   * call ms-data-core-api to validate results files along with peak files
   */
  private void validateFiles() {
    List<String[]> validationCommands = new ArrayList<>();
    List<DataFile> dataFiles = submission.getDataFiles();
    String message = "";

    for (DataFile dataFile : dataFiles) {
      if (dataFile.getFileType().equals(ProjectFileType.RESULT)) {
        validationCommands.add(constructValidationCommand(dataFile, dataFile.getFileFormat()));
        message += "\n\n";
      }
    }
    List<Report> reports = runValidationCommands(validationCommands);
    // todo: make this static
    ValidationReportHTMLFormatUtil formatUtil = new ValidationReportHTMLFormatUtil();
    message = formatUtil.getValidationReportInHTML(submission, reports).toString();
    ValidationResults validationReport = new ValidationResults(message);
  }

  /**
   * This method is a command method to construct mzIdentML, MzTab or PRIDEXML validations
   * using validation tools on ms-data-core-api
   * @param dataFile
   * @param fileFormat
   * @return
   */
  private String[] constructValidationCommand(DataFile dataFile, MassSpecFileFormat fileFormat) {
    File reportFile;
    boolean isFirstPeakFile = true;
    List<String> command = new ArrayList<>();
    String arg_format = (!fileFormat.equals(MassSpecFileFormat.PRIDE))? fileFormat.getFileExtension(): "pridexml";

    try {
      reportFile = File.createTempFile("testResultFile", ".log");
      command.add("-" + ARG_VALIDATION);
      command.add("-" + arg_format);
      command.add(dataFile.getFilePath());
      if (!fileFormat.equals(MassSpecFileFormat.PRIDE)) {
        for (DataFile mappingFile : dataFile.getFileMappings()) {
          if (mappingFile.getFileType().equals(ProjectFileType.PEAK)) {
            command.add(isFirstPeakFile ? "-" + ARG_PEAK : "##");
            command.add(mappingFile.getFilePath());
            isFirstPeakFile = false;
          }
        }
      }
      command.add("-" + ARG_SKIP_SERIALIZATION);
      command.add("-" + ARG_REPORTFILE);
      command.add(reportFile.getAbsolutePath());
    } catch (IOException e) {
      logger.error(
          "Error occurred in construction of MzIdentML validation command: " + e.getMessage());
    }
    return command.stream().toArray(String[]::new);
  }

  /**
   * This method executes the validation commands
   *
   * @param validationCommands List of arrays of String
   * @return String
   */
  private List<Report> runValidationCommands(List<String[]> validationCommands) {
    List<Report> reports = new ArrayList<>();
    try {
      for (String[] args : validationCommands) {
        CommandLine cmd = PGConverter.parseArgs(args);
        reports.add(Validator.startValidation(cmd));
      }
    } catch (ParseException e) {
      logger.error("File validation error: " + e.getMessage());
    }
    return reports;
  }
}
