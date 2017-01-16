/*
 * Created by JFormDesigner on Fri Oct 28 09:31:57 BST 2011
 */

package uk.ac.ebi.pride.gui.form.panel;

import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareHeaderPanel;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.HttpUtil;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Panel for capturing pride login credentials
 *
 * @author Rui Wang
 * @version $Id$
 */
public class PrideLoginPanel extends ContextAwareHeaderPanel {
    public static final String ENTER_KEY_PRESSED = "enter_key_pressed_property";

    private String submitterName;
    private String affiliation;
    private String email;

    public PrideLoginPanel() {
        initComponents();
        initTooltips();
        initKeyListener();
    }

    public String getUserName() {
        return userNameField.getText();
    }

    public void setUserName(String userName) {
        if (userName != null && !userName.trim().equals("")) {
            userNameField.setText(userName);
        }
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public void setPassword(char[] password) {
        if (password != null) {
            passwordField.setText(String.copyValueOf(password));
        }
    }

    public String getSubmitterName() {
        return submitterName;
    }

    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Close the warning balloon tip
     */
    public void hideWarnings() {
        if (warningBalloonTip != null && warningBalloonTip.isVisible()) {
            warningBalloonTip.closeBalloon();
        }
        userNameField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        passwordField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
    }

    public ValidationState doValidation(boolean showWarning) {
        boolean invalid = false;

        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }

        // user name
        if (SubmissionValidator.validateName(userNameField.getText()).hasError()) {
            if (showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(userNameField, appContext.getProperty("pride.login.username.error.message"));
                warningBalloonTip.setVisible(true);
            }
            userNameField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            invalid = true;
        } else {
            userNameField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // password
        if (SubmissionValidator.validatePassword(passwordField.getPassword()).hasError()) {
            if (!invalid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(passwordField, appContext.getProperty("pride.login.password.error.message"));
                warningBalloonTip.setVisible(true);
            }
            passwordField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            invalid = true;
        } else {
            passwordField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        return invalid ? ValidationState.ERROR : ValidationState.SUCCESS;
    }

    private void initTooltips() {
        BalloonTipUtil.createBalloonTooltip(userNameField, appContext.getProperty("pride.login.username.tooltip"));
        BalloonTipUtil.createBalloonTooltip(passwordField, appContext.getProperty("pride.login.password.tooltip"));
    }

    private void initKeyListener() {
        passwordField.addKeyListener(new KeyAdapter() {
            /**
             * Invoked when a key has been pressed.
             */
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    firePropertyChange(ENTER_KEY_PRESSED, false, true);
                }
            }
        });
    }

    private void createUIComponents() {
        newUserButton = GUIUtilities.createLabelLikeButton(null, appContext.getProperty("pride.login.create.account.title"));
        newUserButton.setForeground(ColourUtil.HYPERLINK_COLOUR);
        newUserButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        newUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                HttpUtil.openURL(appContext.getProperty("pride.new.account.url"));
            }
        });
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        createUIComponents();

        userNameLabel = new JLabel();
        userNameField = new JTextField();
        passwordLabel = new JLabel();
        passwordField = new JPasswordField();

        //======== this ========
        setBorder(null);

        //---- userNameLabel ----
        userNameLabel.setText("Email*");
        userNameLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
        userNameLabel.setForeground(Color.white);

        //---- userNameField ----
        userNameField.setFont(new Font("Lucida Grande", Font.PLAIN, 20));

        //---- passwordLabel ----
        passwordLabel.setText("Password*");
        passwordLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 16));
        passwordLabel.setForeground(Color.white);

        //---- passwordField ----
        passwordField.setFont(new Font("Lucida Grande", Font.PLAIN, 20));

        GroupLayout layout = new GroupLayout(this);
        layout.setHonorsVisibility(false);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(0, 335, Short.MAX_VALUE)
                                                .addComponent(newUserButton))
                                        .addComponent(passwordField)
                                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(userNameLabel, GroupLayout.Alignment.LEADING)
                                                        .addComponent(passwordLabel, GroupLayout.Alignment.LEADING))
                                                .addGap(0, 282, Short.MAX_VALUE))
                                        .addComponent(userNameField, GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE))
                                .addContainerGap(12, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(userNameLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(userNameField, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(passwordLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(newUserButton)
                                .addContainerGap(24, Short.MAX_VALUE))
        );
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel userNameLabel;
    private JTextField userNameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    private JButton newUserButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
