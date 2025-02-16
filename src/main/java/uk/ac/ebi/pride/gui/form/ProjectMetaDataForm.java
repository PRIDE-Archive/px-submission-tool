package uk.ac.ebi.pride.gui.form;


import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.gui.form.panel.ProjectMetaDataPanel;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import java.awt.*;

/**
 * This form is responsible for capturing the metadata details
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ProjectMetaDataForm extends Form {
    public static final float DEFAULT_TITLE_FONT_SIZE = 15f;

    private ProjectMetaDataPanel projectMetaDataPanel;

    private JPanel expDescContainer;

    public ProjectMetaDataForm() {
    }

    public JPanel getExpDescContainer() {
        return expDescContainer;
    }
    /**
     * Initialize experiment details panel
     */
    public void initExpDetailsPanel() {
        this.setLayout(new BorderLayout());
        this.expDescContainer = new JPanel(new BorderLayout());

        // title label
        JLabel expDescTitleLabel = new JLabel(appContext.getProperty("experiment.desc.label.title"));
        expDescTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 0));
        expDescTitleLabel.setFont(expDescTitleLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));
        expDescContainer.add(expDescTitleLabel, BorderLayout.NORTH);

        // experiment description panel
        projectMetaDataPanel = new ProjectMetaDataPanel();
        expDescContainer.add(projectMetaDataPanel, BorderLayout.CENTER);

        this.add(expDescContainer, BorderLayout.CENTER);
    }

    @Override
    public ValidationState doValidation() {
        ValidationState expDescState = projectMetaDataPanel.doValidation(true);
        if (expDescState.equals(ValidationState.ERROR)) {
            return ValidationState.ERROR;
        } else {
            return ValidationState.SUCCESS;
        }
    }

    public ProjectMetaData getProjectMetaData() {
        ProjectMetaData metaData = new ProjectMetaData();

        metaData.setProjectTitle(projectMetaDataPanel.getProjectTitle());
        metaData.setKeywords(projectMetaDataPanel.getKeywords());
        metaData.setProjectDescription(projectMetaDataPanel.getProjectDesc());
        metaData.setSampleProcessingProtocol(projectMetaDataPanel.getSampleProcessingProtocol());
        metaData.setDataProcessingProtocol(projectMetaDataPanel.getDataProcessingProtocol());

        return metaData;
    }

    public void setProjectMetaData(ProjectMetaData metaData) {
        if (metaData != null) {
            projectMetaDataPanel.setProjectTitle(metaData.getProjectTitle());
            projectMetaDataPanel.setProjectDesc(metaData.getProjectDescription());
            projectMetaDataPanel.setKeywords(metaData.getKeywords());
            projectMetaDataPanel.setSampleProcessingProtocol(metaData.getSampleProcessingProtocol());
            projectMetaDataPanel.setDataProcessingProtocol(metaData.getDataProcessingProtocol());
        }
    }

    /**
     * Close the warning balloon tip
     */
    public void hideWarnings() {
        projectMetaDataPanel.hideWarnings();
    }
}
