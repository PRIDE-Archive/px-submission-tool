package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.gui.form.panel.LabHeadPanel;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class LabHeadForm extends Form {
    private LabHeadPanel labHeadPanel;

    public LabHeadForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());

        // init experiment details panel
        initLabHeadPanel();
    }

    /**
     * Initialize login panel
     */
    private void initLabHeadPanel() {
        JPanel labHeadContainer = new JPanel();
        labHeadContainer.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // login panel
        constraints.gridx = 0;
        constraints.gridy = 0;
        labHeadPanel = new LabHeadPanel();
        labHeadPanel.setBorder(new LineBorder(Color.GRAY));
        labHeadContainer.add(labHeadPanel, constraints);

        this.add(labHeadContainer, BorderLayout.CENTER);
    }

    @Override
    public ValidationState doValidation() {
        ValidationState expDescState = labHeadPanel.doValidation(true);
        if (expDescState.equals(ValidationState.ERROR)) {
            return ValidationState.ERROR;
        } else {
            return ValidationState.SUCCESS;
        }
    }

    /**
     * Close the warning balloon tip
     */
    public void hideWarnings() {
        labHeadPanel.hideWarnings();
    }

    public String getLabHeadName() {
        return labHeadPanel.getLabHeadName();
    }

    public void setLabHeadName(String name) {
        labHeadPanel.setLabHeadName(name);
    }

    public String getAffiliation() {
        return labHeadPanel.getAffiliation();
    }

    public void setAffiliation(String affiliation) {
        labHeadPanel.setAffiliation(affiliation);
    }

    public String getEmail() {
        return labHeadPanel.getEmail();
    }

    public void setEmail(String email) {
        labHeadPanel.setEmail(email);
    }
}
