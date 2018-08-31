package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.gui.form.action.RemoveFilesAction;
import uk.ac.ebi.pride.gui.form.table.model.FileSelectionTableModel;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.action.AddFileSelectionAction;
import uk.ac.ebi.pride.gui.form.dialog.FileSelectionValidationErrorDialog;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.BorderUtil;
import uk.ac.ebi.pride.gui.util.DataFileValidationMessage;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.help.CSH;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * This form is responsible for file selection or loading pre-existing submission file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileSelectionForm extends Form implements TaskListener<DataFileValidationMessage, Void> {
    private static final float DEFAULT_BUTTON_FONT_SIZE = 14f;
    private static final int DEFAULT_BUTTON_WIDTH = 120;
    private static final int DEFAULT_BUTTON_HEIGHT = 40;

    /**
     * file selection table
     */
    private JTable fileSelectionTable;
    private JButton removeFileButton;

    public FileSelectionForm() {
        initComponents();
    }

    private void initComponents() {
        // setup main pane
        this.setLayout(new BorderLayout());

        // get app context
        appContext = (AppContext) App.getInstance().getDesktopContext();

        // button pane
        JPanel buttonPane = new JPanel(new BorderLayout());
        buttonPane.setBorder(BorderUtil.createLoweredBorder());

        // left button pane
        JPanel leftButtonPane = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // add file button
        JButton addFileButton = new JButton(new AddFileSelectionAction());
        addFileButton.setFont(addFileButton.getFont().deriveFont(DEFAULT_BUTTON_FONT_SIZE));
        addFileButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));

        // remove files button
        removeFileButton = new JButton("Remove Files");
        removeFileButton.setFont(addFileButton.getFont().deriveFont(DEFAULT_BUTTON_FONT_SIZE));
        removeFileButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        removeFileButton.setActionCommand("removeFiles");
        removeFileButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JOptionPane.showMessageDialog(null, "Need to implement!!!");
            }
        });

        leftButtonPane.add(addFileButton);
        leftButtonPane.add(removeFileButton);

        buttonPane.add(leftButtonPane, BorderLayout.WEST);

        // load submission file panel
        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Help button
        // load icon
        Icon helpIcon = GUIUtilities.loadIcon(appContext.getProperty("file.type.help.small.icon"));
        JButton helpButton = GUIUtilities.createLabelLikeButton(helpIcon, appContext.getProperty("file.type.help.question.title"));
        helpButton.setAlignmentY(CENTER_ALIGNMENT);
        helpButton.setToolTipText(appContext.getProperty("file.type.help.question.tooltip"));
        helpButton.setForeground(Color.blue);
        CSH.setHelpIDString(helpButton, "help.file.type");
        helpButton.addActionListener(new CSH.DisplayHelpFromSource(appContext.getMainHelpBroker()));
        helpButton.setPreferredSize(new Dimension(240, 40));
        helpPanel.add(helpButton);

        buttonPane.add(helpPanel, BorderLayout.EAST);
        this.add(buttonPane, BorderLayout.NORTH);

        // file selection table
        fileSelectionTable = TableFactory.createFileSelectionTable();

        // scroll pane
        JScrollPane scrollPane = new JScrollPane(fileSelectionTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(scrollPane, BorderLayout.CENTER);
    }


    @Override
    public ValidationState doValidation() {
        // external validation, see FileScanAndValidationTask
        return null;
    }

    /**
     * Show the warning balloon tip
     *
     * @param errMsg error message
     */
    private void showWarnings(String errMsg) {
        hideWarnings();

        // Create the balloon tip
        JLabel newWarningContents = new JLabel(errMsg);
        newWarningContents.setIcon(GUIUtilities.loadIcon(appContext.getProperty("warning.message.icon")));

        warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(fileSelectionTable, newWarningContents);
        warningBalloonTip.setVisible(true);

        this.revalidate();
        this.repaint();
    }

    /**
     * Get the warning balloon tip
     */
    public void hideWarnings() {
        if (warningBalloonTip != null && warningBalloonTip.isVisible()) {
            warningBalloonTip.closeBalloon();
        }
    }

    @Override
    public void started(TaskEvent<Void> event) {
        // clear previous warning
        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }
    }

    @Override
    public void succeed(TaskEvent<DataFileValidationMessage> event) {
        DataFileValidationMessage message = event.getValue();

        if (message.getState().equals(ValidationState.ERROR)) {
            if (message.getDataFileValidationResults().isEmpty()) {
                showWarnings(message.getMessage());
            } else if (!message.getDataFileValidationResults().isEmpty()) {
                FileSelectionValidationErrorDialog errorDialog = new FileSelectionValidationErrorDialog(((App) App.getInstance()).getMainFrame(), message);
                errorDialog.setLocationRelativeTo(app.getMainFrame());
                errorDialog.setVisible(true);
            }
        }

    }

    @Override
    public void process(TaskEvent<List<Void>> event) {
    }

    @Override
    public void finished(TaskEvent<Void> event) {
        // update table as the file type might change as the result of the validation
        fileSelectionTable.revalidate();
        fileSelectionTable.repaint();
    }

    @Override
    public void failed(TaskEvent<Throwable> event) {
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
