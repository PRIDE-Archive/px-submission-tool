package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.Constant;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.form.table.model.MetaDataTableModel;
import uk.ac.ebi.pride.gui.util.ValidationState;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;

import javax.help.HelpBroker;
import javax.swing.*;
import java.util.Set;

/**
 * Navigation descriptor for AdditionalMetaDataForm
 *
 * @author Rui Wang
 * @version $Id$
 */
public class AdditionalMetaDataDescriptor extends ContextAwareNavigationPanelDescriptor {

    public AdditionalMetaDataDescriptor(String id, String title, String desc) {
        super(id, title, desc, new AdditionalMetaDataForm());
    }

    @Override
    public boolean toSkipPanel() {
        return appContext.getSubmissionType().equals(SubmissionType.COMPLETE);
    }

    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.additional.details", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public void beforeDisplayingPanel() {
        AdditionalMetaDataForm form = (AdditionalMetaDataForm) getNavigationPanel();
        Submission submission = appContext.getSubmissionRecord().getSubmission();
        ProjectMetaData metaData = submission.getProjectMetaData();
        if (metaData != null) {
            form.setSpecies(metaData.getSpecies());
            form.setTissues(metaData.getTissues());
            form.setCellTypes(metaData.getCellTypes());
            form.setDiseases(metaData.getDiseases());
            form.setInstruments(metaData.getInstruments());
            form.setModifications(metaData.getModifications());
            form.setQuantifications(metaData.getQuantifications());
//            form.setComment(metaData.getReasonForPartialSubmission());
        }

        if (appContext.getSubmissionType().equals(SubmissionType.AFFINITY)) {
            MetaDataTableModel instrumentTableModel = new MetaDataTableModel();
            JPanel instrumentPanel = form.initMetadataPanel(appContext.getProperty("instrument.label.title"),
                    true,
                    appContext.getProperty("instrument.affinity.combobox.default.selection"),
                    null,
                    appContext.getProperty("instrument.combobox.default.instrument.file"),
                    Constant.PRIDE,
                    instrumentTableModel);
            form.remove(form.getInstrumentPanel());
            form.setInstrumentPanel(instrumentPanel);
            form.setInstrumentTableModel(instrumentTableModel);
            form.add(instrumentPanel);

            MetaDataTableModel speciesTableModel = new MetaDataTableModel();
            JPanel speciesPanel = form.initMetadataPanel(appContext.getProperty("species.label.title"),
                    true,
                    appContext.getProperty("species.combobox.default.selection"),
                    null,
                    appContext.getProperty("species.combobox.default.species.file"),
                    Constant.NEWT,
                    speciesTableModel);
            form.remove(form.getSpeciesPanel());
            form.setSpeciesPanel(speciesPanel);
            form.setSpeciesTableModel(speciesTableModel);
            form.add(speciesPanel);

            MetaDataTableModel quantTableModel = new MetaDataTableModel();
            JPanel quantPanel = form.initMetadataPanel(appContext.getProperty("quantification.label.title"),
                    true,
                    appContext.getProperty("quantification.combobox.default.selection"),
                    null,
                    appContext.getProperty("quantification.combobox.default.quantification.file"),
                    Constant.PRIDE,
                    quantTableModel);
            form.remove(form.getQuantPanel());
            form.setQuantPanel(quantPanel);
            form.setQuantTableModel(quantTableModel);
            form.add(quantPanel);

            MetaDataTableModel modTableModel = new MetaDataTableModel();
            JPanel modPanel = form.initMetadataPanel(appContext.getProperty("modification.label.title"),
                    true,
                    appContext.getProperty("modification.combobox.default.selection"),
                    null,
                    appContext.getProperty("modification.combobox.default.modification.file"),
                    Constant.PRIDE,
                    modTableModel);
            form.remove(form.getModificationPanel());
            form.setModificationPanel(modPanel);
            form.setModificationTableModel(modTableModel);
            form.add(modPanel);


        } else {
            MetaDataTableModel instrumentTableModel = new MetaDataTableModel();
            JPanel instrumentPanel = form.initMetadataPanel(appContext.getProperty("instrument.label.title"),
                    true,
                    appContext.getProperty("instrument.combobox.default.selection"),
                    appContext.getProperty("instrument.combobox.other.instrument"),
                    appContext.getProperty("instrument.combobox.default.instrument.file"),
                    Constant.PRIDE,
                    instrumentTableModel);
            form.remove(form.getInstrumentPanel());
            form.setInstrumentPanel(instrumentPanel);
            form.setInstrumentTableModel(instrumentTableModel);
            form.add(instrumentPanel);

            MetaDataTableModel speciesTableModel = new MetaDataTableModel();
            JPanel speciesPanel = form.initMetadataPanel(appContext.getProperty("species.label.title"),
                    true,
                    appContext.getProperty("species.combobox.default.selection"),
                    appContext.getProperty("species.combobox.other.species"),
                    appContext.getProperty("species.combobox.default.species.file"),
                    Constant.NEWT,
                    speciesTableModel);
            form.remove(form.getSpeciesPanel());
            form.setSpeciesPanel(speciesPanel);
            form.setSpeciesTableModel(speciesTableModel);
            form.add(speciesPanel);

            MetaDataTableModel quantTableModel = new MetaDataTableModel();
            JPanel quantPanel = form.initMetadataPanel(appContext.getProperty("quantification.label.title"),
                    true,
                    appContext.getProperty("quantification.combobox.default.selection"),
                    appContext.getProperty("quantification.combobox.other.quantification"),
                    appContext.getProperty("quantification.combobox.default.quantification.file"),
                    Constant.PRIDE,
                    quantTableModel);
            form.remove(form.getQuantPanel());
            form.setQuantPanel(quantPanel);
            form.setQuantTableModel(quantTableModel);
            form.add(quantPanel);

            MetaDataTableModel modTableModel = new MetaDataTableModel();
            JPanel modPanel = form.initMetadataPanel(appContext.getProperty("modification.label.title"),
                    true,
                    appContext.getProperty("modification.combobox.default.selection"),
                    appContext.getProperty("modification.combobox.other.modification"),
                    appContext.getProperty("modification.combobox.default.modification.file"),
                    Constant.PRIDE,
                    modTableModel);
            form.remove(form.getModificationPanel());
            form.setModificationPanel(modPanel);
            form.setModificationTableModel(modTableModel);
            form.add(modPanel);
        }
        firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
    }

    @Override
    public void beforeHidingForNextPanel() {
        // validate the content in the table
        AdditionalMetaDataForm form = (AdditionalMetaDataForm) getNavigationPanel();
        ValidationState state = form.doValidation();
        if (!ValidationState.ERROR.equals(state)) {
            // save user input
            saveFormContent();

            // hide warnings
            form.hideWarnings();

            // notify success
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
        } else {
            // notify validation error
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
        }
    }

    @Override
    public void beforeHidingForPreviousPanel() {
        //hide any visible warning balloon tip
        AdditionalMetaDataForm form = (AdditionalMetaDataForm) getNavigationPanel();
        form.hideWarnings();
        saveFormContent();

        firePropertyChange(BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY, false, true);
    }

    /**
     * Save the content from the form to AppContext
     */
    private void saveFormContent() {
        AdditionalMetaDataForm form = (AdditionalMetaDataForm) getNavigationPanel();
        Submission submission = appContext.getSubmissionRecord().getSubmission();
        ProjectMetaData metaData = submission.getProjectMetaData();
        if (metaData == null) {
            metaData = new ProjectMetaData();
            submission.setProjectMetaData(metaData);
        }

        // species
        Set<CvParam> species = form.getSpecies();
        metaData.clearSpecies();
        metaData.addSpecies(species.toArray(new CvParam[species.size()]));

        // tissues
        Set<CvParam> tissues = form.getTissues();
        metaData.clearTissues();
        metaData.addTissues(tissues.toArray(new CvParam[tissues.size()]));

        // cell type
        Set<CvParam> cellTypes = form.getCellTypes();
        metaData.clearCellTypes();
        metaData.addCellTypes(cellTypes.toArray(new CvParam[cellTypes.size()]));

        // disease
        Set<CvParam> diseases = form.getDiseases();
        metaData.clearDiseases();
        metaData.addDiseases(diseases.toArray(new CvParam[diseases.size()]));

        // instrument
        Set<CvParam> instruments = form.getInstruments();
        metaData.clearInstruments();
        metaData.addInstruments(instruments.toArray(new CvParam[instruments.size()]));

        // modification
        Set<CvParam> modifications = form.getModifications();
        metaData.clearModifications();
        metaData.addModifications(modifications.toArray(new CvParam[modifications.size()]));

        // quantification
        Set<CvParam> quantifications = form.getQuantifications();
        metaData.clearQuantifications();
        metaData.addQuantifications(quantifications.toArray(new CvParam[quantifications.size()]));

    }
}
