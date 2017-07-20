package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.form.panel.PrideLoginPanel;
import uk.ac.ebi.pride.gui.navigation.Navigator;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class PrideLoginForm extends Form {

    private PrideLoginPanel prideLoginPanel;

    public PrideLoginForm() {
        initComponents();
        initPropertyListener();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());

        // init experiment details panel
        initPrideLoginPanel();
    }

    /**
     * Trigger login action when enter key is pressed
     */
    private void initPropertyListener() {
        prideLoginPanel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equalsIgnoreCase(PrideLoginPanel.ENTER_KEY_PRESSED)) {
                    Navigator navigator = ((App) App.getInstance()).getNavigator();
                    JButton nextButton = navigator.getNextButton();
                    nextButton.doClick();
                }
            }
        });
    }

    /**
     * Initialize login panel
     */
    private void initPrideLoginPanel() {
        JPanel loginContainer = new JPanel();
        loginContainer.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // login panel
        constraints.gridx = 0;
        constraints.gridy = 0;
        prideLoginPanel = new PrideLoginPanel();
        prideLoginPanel.setBorder(new LineBorder(Color.GRAY));
        loginContainer.add(prideLoginPanel, constraints);

        this.add(loginContainer, BorderLayout.CENTER);
    }

    @Override
    public ValidationState doValidation() {
        ValidationState expDescState = prideLoginPanel.doValidation(true);
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
        prideLoginPanel.hideWarnings();
    }

    public String getUserName() {
        return prideLoginPanel.getUserName();
    }

    public void setUserName(String username) {
        prideLoginPanel.setUserName(username);
    }

    public char[] getPassword() {
        return prideLoginPanel.getPassword();
    }

    public void setPassword(char[] password) {
        prideLoginPanel.setPassword(password);
    }

    public String getSubmitterName() {
        return prideLoginPanel.getSubmitterName();
    }

    public void setSubmitterName(String name) {
        prideLoginPanel.setSubmitterName(name);
    }

    public String getAffiliation() {
        return prideLoginPanel.getAffiliation();
    }

    public void setAffiliation(String affiliation) {
        prideLoginPanel.setAffiliation(affiliation);
    }

    public String getEmail() {
        return prideLoginPanel.getEmail();
    }

    public void setEmail(String email) {
        prideLoginPanel.setEmail(email);
    }

    public String getCountry() {
        return prideLoginPanel.getCountry();
    }

    public void setCountry(String country) {
        prideLoginPanel.setCountry(country);
    }

    public String getOrcid() {
        return prideLoginPanel.getOrcid();
    }

    public void setOrcid(String orcid) {
        prideLoginPanel.setOrcid(orcid);
    }
}
