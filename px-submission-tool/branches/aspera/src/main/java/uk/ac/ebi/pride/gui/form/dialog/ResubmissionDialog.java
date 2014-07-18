package uk.ac.ebi.pride.gui.form.dialog;

import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareDialog;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;
import uk.ac.ebi.pride.gui.form.panel.ResubmissionPanel;
import uk.ac.ebi.pride.gui.navigation.NavigationControlPanel;
import uk.ac.ebi.pride.gui.util.BorderUtil;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.help.HelpBroker;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for handling the resubmission
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ResubmissionDialog extends ContextAwareDialog implements ActionListener {
    private static final String RESUBMISSION_CHECKED_COMMAND = "resubmissionAction";
    private static final String HELP_ACTION_COMMAND = "helpAction";
    private static final String CANCEL_ACTION_COMMAND = "cancelAction";
    private static final String SET_ACTION_COMMAND = "setAction";

    /**
     * Checkbox to enable/disable resubmission
     */
    private JCheckBox resubmissionCheckBox;

    /**
     * Panel contains all the resubmission related components
     */
    private ResubmissionPanel resubmissionPanel;

    /**
     * Button to confirm the resubmission details
     */
    private JButton addButton;

    public ResubmissionDialog(Frame owner) {
        super(owner);
        initComponents();
    }

    public ResubmissionDialog(Dialog owner) {
        super(owner);
        initComponents();
    }

    /**
     * Create GUI components
     */
    private void initComponents() {
        this.setSize(new Dimension(380, 320));

        this.setTitle(appContext.getProperty("resubmission.dialog.title"));

        JPanel contentPanel = new JPanel(new BorderLayout());
        this.setContentPane(contentPanel);

        // create table panel
        initResubmissionPanel();

        // create button panel
        initControlPanel();

        this.setContentPane(contentPanel);
    }

    /**
     * Initialize input text field panel
     */
    private void initResubmissionPanel() {
        JPanel resubmissionContainerPanel = new JPanel(new BorderLayout());
        resubmissionContainerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        resubmissionCheckBox = new JCheckBox(appContext.getProperty("resubmission.checkbox.label"), false);
        resubmissionCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        resubmissionCheckBox.setActionCommand(RESUBMISSION_CHECKED_COMMAND);
        resubmissionCheckBox.addActionListener(this);
        resubmissionContainerPanel.add(resubmissionCheckBox, BorderLayout.NORTH);

        resubmissionPanel = new ResubmissionPanel();
        resubmissionPanel.setBorder(BorderUtil.createLoweredBorder());
        resubmissionPanel.setEnabled(false);

        resubmissionContainerPanel.add(resubmissionPanel, BorderLayout.CENTER);

        this.getContentPane().add(resubmissionContainerPanel, BorderLayout.CENTER);
    }

    /**
     * Initialize control panel
     */
    private void initControlPanel() {
        // setup main pane
        JPanel controlPanel = new NavigationControlPanel();
        controlPanel.setLayout(new BorderLayout());

        // help button
        JPanel helpButtonPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));
        JButton helpButton = GUIUtilities.createLabelLikeButton(GUIUtilities.loadIcon(appContext.getProperty("help.button.small.icon")), null);
        helpButton.setPreferredSize(new Dimension(90, 30));
        helpButton.setFocusable(false);
        helpButton.setActionCommand(HELP_ACTION_COMMAND);
        helpButton.addActionListener(this);
        helpButtonPanel.add(helpButton);
        controlPanel.add(helpButtonPanel, BorderLayout.WEST);

        // control pane
        JPanel ctrlPane = new NonOpaquePanel(new FlowLayout(FlowLayout.RIGHT));

        // cancel button
        JButton cancelButton = new JButton(appContext.getProperty("cancel.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("cancel.button.small.icon")));
        cancelButton.setPreferredSize(new Dimension(90, 30));
        cancelButton.setActionCommand(CANCEL_ACTION_COMMAND);
        cancelButton.addActionListener(this);
        ctrlPane.add(cancelButton);

        // ok button
        addButton = new JButton(appContext.getProperty("ok.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("ok.button.small.icon")));
        addButton.setPreferredSize(new Dimension(90, 30));
        addButton.setActionCommand(SET_ACTION_COMMAND);
        addButton.addActionListener(this);
        addButton.setEnabled(false);
        ctrlPane.add(addButton);

        controlPanel.add(ctrlPane, BorderLayout.EAST);

        this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String evtName = e.getActionCommand();

        if (HELP_ACTION_COMMAND.equals(evtName)) {
            HelpBroker hb = appContext.getMainHelpBroker();
            hb.showID("help.resubmission", "javax.help.SecondaryWindow", "main");
        } else if (CANCEL_ACTION_COMMAND.equals(evtName)) {
            this.setVisible(false);
        } else if (SET_ACTION_COMMAND.equals(evtName)) {
            Submission submission = appContext.getSubmissionRecord().getSubmission();
            ProjectMetaData projectMetaData = submission.getProjectMetaData();
            if (resubmissionCheckBox.isSelected()) {
                ValidationState validateState = resubmissionPanel.doValidation();
                if (validateState.equals(ValidationState.SUCCESS)) {
                    // update app context with the login details and resubmission accession
                    projectMetaData.setResubmissionPxAccession(resubmissionPanel.getPxAccession());
                    projectMetaData.getSubmitterContact().setUserName(resubmissionPanel.getUserName());
                    projectMetaData.getSubmitterContact().setEmail(resubmissionPanel.getUserName());
                    projectMetaData.getSubmitterContact().setPassword(resubmissionPanel.getPassword());
                    this.setVisible(false);
                }
            } else {
                // remove resubmission related details
                projectMetaData.setResubmissionPxAccession(null);
                projectMetaData.getSubmitterContact().setUserName(null);
                projectMetaData.getSubmitterContact().setEmail(null);
                projectMetaData.getSubmitterContact().setPassword(null);
                this.setVisible(false);
            }

        } else if (RESUBMISSION_CHECKED_COMMAND.equals(evtName)) {
            resubmissionPanel.setEnabled(resubmissionCheckBox.isSelected());
            addButton.setEnabled(true);
        }
    }

    /**
     * Update the state and the content of the components
     */
    public void updateState() {
        Submission submission = appContext.getSubmissionRecord().getSubmission();
        if (submission.getProjectMetaData().getResubmissionPxAccession() == null) {
            resubmissionCheckBox.setSelected(false);
            resubmissionPanel.setEnabled(false);
            resubmissionPanel.setUserName(null);
            resubmissionPanel.setPassword(null);
            resubmissionPanel.setPxAccession(null);
        } else {
            resubmissionCheckBox.setSelected(true);
            resubmissionPanel.setEnabled(true);
            resubmissionPanel.setUserName(submission.getProjectMetaData().getSubmitterContact().getEmail());
            resubmissionPanel.setPassword(submission.getProjectMetaData().getSubmitterContact().getPassword());
            resubmissionPanel.setPxAccession(submission.getProjectMetaData().getResubmissionPxAccession());
        }
    }
}
