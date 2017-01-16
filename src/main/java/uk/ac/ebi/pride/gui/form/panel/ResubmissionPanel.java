/*
 * Created by JFormDesigner on Wed Aug 08 15:00:00 BST 2012
 */

package uk.ac.ebi.pride.gui.form.panel;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.task.GetPXSubmissionDetailTask;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author Rui Wang
 *         <p/>
 *         todo: need more testing with wrong login credentials after first time login
 */
public class ResubmissionPanel extends ContextAwarePanel implements ActionListener, TaskListener<java.util.Set<String>, String> {

    private static final String LOGIN_ACTION = "login";

    /**
     * PRIDE user name and password
     * Note: this should be only updated when a login was successful
     */
    private String userName;
    private char[] password;
    private boolean loggedIn;

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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String usr) {
        userName = usr;
        userNameField.setText(userName);
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] pwd) {
        password = pwd;
        if (pwd != null) {
            passwordField.setText(String.valueOf(pwd));
        } else {
            passwordField.setText(null);
        }
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

        userNameField.setBackground(Color.white);
        passwordField.setBackground(Color.white);

        if (pxAccession == null) {
            if (pxDatasetComboBox.getItemCount() > 1) {
                // show warning on combo box
                showWarning(pxDatasetComboBox, appContext.getProperty("resubmission.dataset.selection.warning.message"));
            } else if (userNameField.getText() == null || userNameField.getText().trim().equals("")) {
                // show warning on user name field
                showWarning(userNameField, appContext.getProperty("resubmission.user.name.warning.message"));
            } else if (passwordField.getPassword() == null || passwordField.getPassword().length == 0) {
                // show warning on password field
                showWarning(passwordField, appContext.getProperty("resubmission.password.warning.message"));
            } else {
                if (loggedIn) {
                    // show warning on combo box
                    showWarning(pxDatasetComboBox, appContext.getProperty("resubmission.private.dataset.warning.message"));
                } else {
                    // show warning on login button
                    showWarning(loginButton, appContext.getProperty("resubmission.login.warning.message"));
                }
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
            GetPXSubmissionDetailTask getPXSubmissionDetailTask = new GetPXSubmissionDetailTask(userNameField.getText(), String.valueOf(passwordField.getPassword()));
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
    public void succeed(TaskEvent<java.util.Set<String>> event) {
        java.util.Set<String> pxAccessions = event.getValue();

        pxDatasetComboBox.removeAllItems();

        // update user name and password
        if (pxAccessions.size() == 0) {
            userName = null;
            password = null;
            pxDatasetComboBox.addItem(appContext.getProperty("resubmission.no.private.px.label"));
        } else {
            pxDatasetComboBox.addItem(appContext.getProperty("resubmission.select.px.dataset.label"));

            // update the px combo box
            for (String s : pxAccessions) {
                pxDatasetComboBox.addItem(s);
            }

            // update user name and password
            userName = userNameField.getText();
            password = passwordField.getPassword();
        }

        // change boolean the indicate the login button has been pressed
        loggedIn = true;
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
        loginButton.setText("Login");

        //---- pxDatasetLabel ----
        pxDatasetLabel.setText("Select the dataset to be replaced");
        pxDatasetLabel.setFont(pxDatasetLabel.getFont().deriveFont(pxDatasetLabel.getFont().getStyle() | Font.BOLD));

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addComponent(separator1)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup()
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup()
                                                        .addComponent(userNameField, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(userNameLabel, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
                                                .addGroup(layout.createParallelGroup()
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGap(21, 21, 21)
                                                                .addComponent(passwordLabel))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE))))
                                        .addComponent(pxDatasetLabel)
                                        .addComponent(pxDatasetComboBox, GroupLayout.PREFERRED_SIZE, 226, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(loginButton, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(passwordLabel)
                                        .addComponent(userNameLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(userNameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(loginButton)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(separator1, GroupLayout.PREFERRED_SIZE, 5, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
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
