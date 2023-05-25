/*
 * Created by JFormDesigner on Wed Aug 08 15:00:00 BST 2012
 */

package uk.ac.ebi.pride.gui.form.panel;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.task.GetPXSubmissionDetailTask;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.ValidationState;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author Rui Wang
 *         <p/>
 *         todo: need more testing with wrong login credentials after first time login
 */
public class ResubmissionPanel extends ContextAwarePanel implements ActionListener, TaskListener<HashMap<String, SubmissionTypeConstants>, String> {

    private static final String LOGIN_ACTION = "login";
    private HashMap<String, SubmissionTypeConstants> projectDetails;

    public ResubmissionPanel() {
        initComponents();
        postInitialization();
    }

    private void postInitialization() {
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("resubmission.login.button.lock.small.icon"));
        loginButton.setIcon(icon);

        loginButton.setActionCommand(LOGIN_ACTION);
        loginButton.addActionListener(this);

        // init warning balloon
        this.warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(userNameField, "");
        this.warningBalloonTip.setPadding(2);
        this.warningBalloonTip.setVisible(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component component : getComponents())
            component.setEnabled(enabled);
    }

    public String getPxAccession() {
        String pxAccession = (String) pxDatasetComboBox.getSelectedItem();

        if (pxAccession != null) {
            Matcher matcher = Constant.PX_ACC_PATTERN.matcher(pxAccession);
            if (matcher.matches()) {
                return pxAccession;
            }
        }
        return null;
    }

    /**
     * Get Submission type (with a type conversion to support data-provider-api) by Px Accession
     * @param pxAccession project accession
     * @return Submission Type
     */
    public SubmissionType getSubmissionType(String pxAccession){
        return SubmissionType.valueOf(projectDetails.get(pxAccession).getName());
    }

    public void setPxAccession(String pxAccession) {
        pxDatasetComboBox.removeAllItems();
        if (pxAccession != null) {
            pxDatasetComboBox.addItem(pxAccession);
        }
    }

    public ValidationState doValidation() {
        String pxAccession = getPxAccession();

        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }
        if (pxAccession == null) {
            if (pxDatasetComboBox.getItemCount() > 1) {
                // show warning on combo box
                showWarning(pxDatasetComboBox, appContext.getProperty("resubmission.dataset.selection.warning.message"));
            }
            return ValidationState.ERROR;
        }
        return ValidationState.SUCCESS;
    }

    /**
     * Show warning message
     *
     * @param component attachment component
     * @param message   warning message
     */
    private void showWarning(JComponent component, String message) {
        if (component instanceof JTextField) {
            component.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
        }
        // set attachment component
        warningBalloonTip.setAttachedComponent(component);

        // show balloon warning
        warningBalloonTip.setContents(new JLabel(message));
        warningBalloonTip.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (LOGIN_ACTION.equals(e.getActionCommand())) {
            GetPXSubmissionDetailTask getPXSubmissionDetailTask = new GetPXSubmissionDetailTask(
                    appContext.getSubmissionRecord().getUserName(), appContext.getSubmissionRecord().getPassword());
            getPXSubmissionDetailTask.addTaskListener(this);
            getPXSubmissionDetailTask.setGUIBlocker(new DefaultGUIBlocker(getPXSubmissionDetailTask, GUIBlocker.Scope.NONE, null));
            App.getInstance().getDesktopContext().addTask(getPXSubmissionDetailTask);
            warningBalloonTip.setVisible(false);
        }
    }

    @Override
    public void started(TaskEvent<Void> event) {
        // set the login button to loading icon
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("resubmission.login.button.loading.small.icon"));
        loginButton.setIcon(icon);
    }

    @Override
    public void succeed(TaskEvent<HashMap<String, SubmissionTypeConstants>> event) {
        projectDetails = event.getValue();

        pxDatasetComboBox.removeAllItems();

        if (projectDetails.size() == 0) {
            pxDatasetComboBox.addItem(appContext.getProperty("resubmission.no.private.px.label"));
        } else {
            pxDatasetComboBox.addItem(appContext.getProperty("resubmission.select.px.dataset.label"));
            // update the px combo box
            for (Map.Entry<String, SubmissionTypeConstants> project : projectDetails.entrySet()) {
                pxDatasetComboBox.addItem(project.getKey());
            }
        }
    }

    @Override
    public void finished(TaskEvent<Void> event) {
        // set the login button to normal icon
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("resubmission.login.button.lock.small.icon"));
        loginButton.setIcon(icon);
    }

    @Override
    public void process(TaskEvent<List<String>> event) {
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

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        userNameLabel = new JLabel();
        userNameField = new JTextField();
        passwordLabel = new JLabel();
        passwordField = new JPasswordField();
        loginButton = new JButton();
        separator1 = new JSeparator();
        pxDatasetLabel = new JLabel();
        pxDatasetComboBox = new JComboBox();

        //======== this ========

        //---- userNameLabel ----
        userNameLabel.setText("Email");
        userNameLabel.setFont(userNameLabel.getFont().deriveFont(userNameLabel.getFont().getStyle() | Font.BOLD));

        //---- passwordLabel ----
        passwordLabel.setText("Password");
        passwordLabel.setFont(passwordLabel.getFont().deriveFont(passwordLabel.getFont().getStyle() | Font.BOLD));

        //---- loginButton ----
        loginButton.setText("Load Private Datasets");

        //---- pxDatasetLabel ----
        pxDatasetLabel.setText("Select the dataset to be replaced");
        pxDatasetLabel.setFont(pxDatasetLabel.getFont().deriveFont(pxDatasetLabel.getFont().getStyle() | Font.BOLD));

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
//                        .addComponent(separator1)
//                        .addGroup(layout.createSequentialGroup()
//                                .addContainerGap()
//                                .addGroup(layout.createParallelGroup()
//                                        .addGroup(layout.createSequentialGroup()
//                                                .addGroup(layout.createParallelGroup()
//                                                        .addComponent(userNameField, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE)
//                                                        .addComponent(userNameLabel, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
//                                                .addGroup(layout.createParallelGroup()
//                                                        .addGroup(layout.createSequentialGroup()
//                                                                .addGap(21, 21, 21)
//                                                                .addComponent(passwordLabel))
//                                                        .addGroup(layout.createSequentialGroup()
//                                                                .addGap(18, 18, 18)
//                                                                .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE))))
                                        .addComponent(pxDatasetLabel)
                                        .addComponent(pxDatasetComboBox, GroupLayout.PREFERRED_SIZE, 226, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(loginButton, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
//                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
//                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
//                                        .addComponent(passwordLabel)
//                                        .addComponent(userNameLabel))
//                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
//                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
//                                        .addComponent(userNameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
//                                        .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
//                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loginButton)
//                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
//                                .addComponent(separator1, GroupLayout.PREFERRED_SIZE, 5, GroupLayout.PREFERRED_SIZE)
//                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pxDatasetLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pxDatasetComboBox, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(18, Short.MAX_VALUE))
        );
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel userNameLabel;
    private JTextField userNameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JSeparator separator1;
    private JLabel pxDatasetLabel;
    private JComboBox pxDatasetComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
