/*
 * Created by JFormDesigner on Thu Aug 08 10:07:56 BST 2013
 */

package uk.ac.ebi.pride.gui.form.panel;

import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareHeaderPanel;
import uk.ac.ebi.pride.gui.form.comp.HintedTextArea;
import uk.ac.ebi.pride.gui.form.comp.HintedTextField;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import java.awt.*;

/**
 * @author User #2
 */
public class LabHeadPanel extends ContextAwareHeaderPanel {

    private String nameHint;
    private String emailHint;
    private String affiliationHint;

    public LabHeadPanel() {
        initComponents();
        initTooltips();
    }

    public ValidationState doValidation(boolean showWarning) {
        boolean invalid = false;

        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }

        // name
        String name = nameTextField.getText();
        if (nameHint.equals(name) || SubmissionValidator.validateName(name).hasError()) {
            if (showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(nameTextField, appContext.getProperty("lab.head.name.error.message"));
                warningBalloonTip.setVisible(true);
            }
            nameTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            invalid = true;
        } else {
            nameTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // email
        String email = emailTextField.getText();
        if (emailHint.equals(email) || SubmissionValidator.validateEmail(email).hasError()) {
            if (!invalid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(emailTextField, appContext.getProperty("lab.head.email.error.message"));
                warningBalloonTip.setVisible(true);
            }
            emailTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            invalid = true;
        } else {
            emailTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // affiliation
        String affiliation = affiliationTextArea.getText();
        if (affiliationHint.equals(affiliation) || SubmissionValidator.validateAffiliation(affiliation).hasError()) {
            if (!invalid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(affiliationTextArea, appContext.getProperty("lab.head.affiliation.error.message"));
                warningBalloonTip.setVisible(true);
            }
            affiliationTextArea.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            invalid = true;
        } else {
            affiliationTextArea.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        return invalid ? ValidationState.ERROR : ValidationState.SUCCESS;
    }

    public void hideWarnings() {
        if (warningBalloonTip != null && warningBalloonTip.isVisible()) {
            warningBalloonTip.closeBalloon();
        }
        nameTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        emailTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        affiliationTextArea.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
    }

    public String getLabHeadName() {
        return nameTextField.getText();
    }

    public void setLabHeadName(String submitterName) {
        nameTextField.setText(submitterName);
    }

    public String getAffiliation() {
        return affiliationTextArea.getText();
    }

    public void setAffiliation(String affiliation) {
        affiliationTextArea.setText(affiliation);
    }

    public String getEmail() {
        return emailTextField.getText();
    }

    public void setEmail(String email) {
        emailTextField.setText(email);
    }

    private void initTooltips() {
        BalloonTipUtil.createBalloonTooltip(nameTextField, appContext.getProperty("lab.head.name.tooltip"));
        BalloonTipUtil.createBalloonTooltip(emailTextField, appContext.getProperty("lab.head.email.tooltip"));
        BalloonTipUtil.createBalloonTooltip(affiliationTextArea, appContext.getProperty("lab.head.affiliation.tooltip"));
    }

    private void createUIComponents() {
        this.nameHint = appContext.getProperty("lab.head.name.hint");
        nameTextField = new HintedTextField(nameHint);

        this.emailHint = appContext.getProperty("lab.head.email.hint");
        emailTextField = new HintedTextField(emailHint);

        this.affiliationHint = appContext.getProperty("lab.head.affiliation.hint");
        affiliationTextArea = new HintedTextArea(affiliationHint);

        notificationLabel = new JLabel(appContext.getProperty("lab.head.explanation.desc"));
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        createUIComponents();

        nameLabel = new JLabel();
        emailLabel = new JLabel();
        affiliationLabel = new JLabel();
        scrollPane1 = new JScrollPane();

        //======== this ========

        //---- nameLabel ----
        nameLabel.setText("Name*");
        nameLabel.setFont(new Font("sansserif", Font.PLAIN, 16));
        nameLabel.setForeground(Color.white);

        //---- emailLabel ----
        emailLabel.setText("Email*");
        emailLabel.setFont(new Font("sansserif", Font.PLAIN, 16));
        emailLabel.setForeground(Color.white);

        //---- affiliationLabel ----
        affiliationLabel.setText("Affiliation*");
        affiliationLabel.setFont(new Font("sansserif", Font.PLAIN, 16));
        affiliationLabel.setForeground(Color.white);

        //======== scrollPane1 ========
        {
            scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- affiliationTextArea ----
            affiliationTextArea.setLineWrap(true);
            scrollPane1.setViewportView(affiliationTextArea);
        }

        //---- notificationLabel ----
        notificationLabel.setForeground(Color.white);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(scrollPane1)
                                        .addComponent(emailTextField, GroupLayout.Alignment.LEADING)
                                        .addComponent(nameTextField, GroupLayout.Alignment.LEADING)
                                        .addComponent(notificationLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup()
                                                        .addComponent(nameLabel)
                                                        .addComponent(emailLabel)
                                                        .addComponent(affiliationLabel))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(nameLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(nameTextField, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
                                .addGap(16, 16, 16)
                                .addComponent(emailLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(emailTextField, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(affiliationLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(notificationLabel, GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
                                .addContainerGap())
        );
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel nameLabel;
    private JTextField nameTextField;
    private JLabel emailLabel;
    private JTextField emailTextField;
    private JLabel affiliationLabel;
    private JScrollPane scrollPane1;
    private JTextArea affiliationTextArea;
    private JLabel notificationLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
