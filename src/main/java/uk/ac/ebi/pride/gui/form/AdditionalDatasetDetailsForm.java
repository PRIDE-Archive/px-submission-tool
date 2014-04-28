package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.gui.form.panel.AdditionalProjectMetadataPanel;
import uk.ac.ebi.pride.gui.util.ValidationState;

import java.awt.*;
import java.util.Collection;
import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class AdditionalDatasetDetailsForm extends Form {

    private AdditionalProjectMetadataPanel additionalProjectMetadataPanel;

    public AdditionalDatasetDetailsForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());

        additionalProjectMetadataPanel = new AdditionalProjectMetadataPanel();
        this.add(additionalProjectMetadataPanel, BorderLayout.CENTER);

    }

    @Override
    public ValidationState doValidation() {
        return additionalProjectMetadataPanel.doValidation(true);
    }

    public void hideWarnings() {
        additionalProjectMetadataPanel.hideWarnings();
    }

    public Set<String> getSelectedProjectTags() {
        return additionalProjectMetadataPanel.getSelectedProjectTags();
    }

    public void setSelectedProjectTags(Collection<String> projectTags) {
        additionalProjectMetadataPanel.setSelectedProjectTags(projectTags);
    }

    public Set<String> getPubMedIds() {
        return additionalProjectMetadataPanel.getPubMedIds();
    }

    public void setPubMedIds(Collection<String> pubMedIds) {
        additionalProjectMetadataPanel.setPubMedIds(pubMedIds);
    }

    public Set<String> getReanalysisPXAccessions() {
        return additionalProjectMetadataPanel.getReanalysisPXAccessions();
    }

    public void setReanalysisPXAccessions(Collection<String> pxAccessions) {
        additionalProjectMetadataPanel.setReanalysisPXAccessions(pxAccessions);
    }

    public String getOtherOmicsDatasetLink() {
        return additionalProjectMetadataPanel.getOtherOmicsDatasetLink();
    }

    public void setOtherOmicsDatasetLink(String otherOmicsDatasetLink) {
        additionalProjectMetadataPanel.setOtherOmicsDatasetLink(otherOmicsDatasetLink);
    }
}
