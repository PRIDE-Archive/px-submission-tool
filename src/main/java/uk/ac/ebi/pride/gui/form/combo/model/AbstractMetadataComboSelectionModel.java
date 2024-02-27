package uk.ac.ebi.pride.gui.form.combo.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;

import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.gui.util.CvFileUtil;
import uk.ac.ebi.pride.toolsuite.gui.EDTUtils;
import uk.ac.ebi.pride.toolsuite.ols.dialog.OLSDialog;
import uk.ac.ebi.pride.toolsuite.ols.dialog.OLSInputable;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Identifier;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.ebi.pride.data.util.Constant.PRIDE;

/**
 * Abstract combo selection model for metadata selection
 *
 * @author Rui Wang
 * @version $Id$
 */
public abstract class AbstractMetadataComboSelectionModel extends AbstractListModel implements ComboBoxModel, OLSInputable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMetadataComboSelectionModel.class);

    protected List elements;
    protected Object selectedElement;
    protected AppContext appContext;
    private String ontology;
    private String otherSelectionOption;

    protected AbstractMetadataComboSelectionModel(String defaultSelectionOption,
                                                  String otherSelectionOption,
                                                  String defaultValueFile,
                                                  String ontology) {
        this.elements = new ArrayList();
        this.appContext = (AppContext) App.getInstance().getDesktopContext();

        // add default selection element
        elements.add(defaultSelectionOption);

        // default selected item
        selectedElement = elements.get(0);

        // add all the element from a template file
        try {
            Collection<CvParam> cvParams = CvFileUtil.parseByTabDelimitedLine(defaultValueFile);
            SubmissionTypeConstants submissionType = appContext.getSubmissionType();

            if (submissionType.equals(SubmissionTypeConstants.AFFINITY)) {
                otherSelectionOption = null;
                if (defaultValueFile.contains("species")) {
                    elements.addAll(cvParams.stream().filter(cvParam -> cvParam.getAccession().equals("9606") ||
                            cvParam.getAccession().equals("10090")).collect(Collectors.toList()));
                }
                if (defaultValueFile.contains("quantification")) {
                    elements.addAll(cvParams.stream().filter(cvParam -> cvParam.getAccession().equals("PRIDE:0000651") ||
                            cvParam.getAccession().equals("PRIDE:0000652") || cvParam.getAccession().equals("PRIDE:0000653")).collect(Collectors.toList()));
                }
                if (defaultValueFile.contains("instrument")) {
                    elements.addAll(cvParams.stream().filter(cvParam -> cvParam.getCvLabel().equals(PRIDE)).collect(Collectors.toList()));
                }
                if (defaultValueFile.contains("modification")) {
                    elements.addAll(cvParams.stream().filter(cvParam -> cvParam.getCvLabel().equals(PRIDE)).collect(Collectors.toList()));
                }
            } else {
                if (defaultValueFile.contains("instrument")) {
                    elements.addAll(cvParams.stream().filter(cvParam -> !cvParam.getCvLabel().equals(PRIDE)).collect(Collectors.toList()));
                } else {
                    elements.addAll(cvParams);
                }
            }
        } catch (IOException ioe) {
            logger.error("Failed to load default values from the template file", ioe);
        }

        // add element for calling ols dialog
        if (otherSelectionOption != null) {
            this.otherSelectionOption = otherSelectionOption;
            elements.add(otherSelectionOption);
        }

        // default ontology to check
        this.ontology = ontology;
    }

    public abstract void addItem(CvParam cvParam);

    @Override
    public void setSelectedItem(Object anItem) {

        if ((selectedElement != null && !selectedElement.equals(anItem)) || selectedElement == null && anItem != null) {
            if (anItem instanceof CvParam) {
                addItem((CvParam) anItem);
            } else if (otherSelectionOption != null && otherSelectionOption.equals(anItem)) {
                final Map<String, List<Identifier>> preselectedOntologies = new HashMap<>();
                if (((String) anItem).equalsIgnoreCase(appContext.getProperty("species.combobox.other.species"))) {
                    preselectedOntologies.put("ncbitaxon", null);
                    ontology = "ncbitaxon";
                } else if (((String) anItem).equalsIgnoreCase(appContext.getProperty("instrument.combobox.other.instrument"))) {
                    preselectedOntologies.put("ms", null);
                    ontology = "ms";
                } else if (((String) anItem).equalsIgnoreCase(appContext.getProperty("modification.combobox.other.modification"))) {
                    preselectedOntologies.put("mod", null);
                    ontology = "mod";
                } else if (((String) anItem).equalsIgnoreCase(appContext.getProperty("quantification.combobox.other.quantification"))) {
                    preselectedOntologies.put("pride", null);
                    ontology = "pride";
                } else if (((String) anItem).equalsIgnoreCase(appContext.getProperty("tissue.combobox.other.tissue"))) {
                    preselectedOntologies.put("bto", null);
                    preselectedOntologies.put("efo", null);
                    ontology = "bto";
                } else if (((String) anItem).equalsIgnoreCase(appContext.getProperty("celltype.combobox.other.celltype"))) {
                    preselectedOntologies.put("cl", null);
                    preselectedOntologies.put("efo", null);
                    ontology = "cl";
                } else if (((String) anItem).equalsIgnoreCase(appContext.getProperty("disease.combobox.other.disease"))) {
                    preselectedOntologies.put("doid", null);
                    preselectedOntologies.put("efo", null);
                    ontology = "doid";
                }
                Runnable run = () -> new OLSDialog(((App) App.getInstance()).getMainFrame(), AbstractMetadataComboSelectionModel.this, true, "", ontology, "", preselectedOntologies, true);
                EDTUtils.invokeLater(run);
            }
        }
    }

    /**
     * Inserts the selected cv term into the parent frame or dialog. If the
     * frame (or dialog) contains more than one OLS term, the field label can be
     * used to separate between the two. Modified row is used if the cv terms
     * are in a table and one of them are altered.
     *
     * @param field         the name of the field where the CV term will be inserted
     * @param selectedValue the value to search for
     * @param accession     the accession number to search for
     * @param ontologyShort short name of the ontology to search in, e.g., GO or
     *                      MOD
     * @param ontologyLong  long ontology name, e.g., Gene Ontology [GO]
     * @param modifiedRow   if the CV terms is going to be inserted into a table,
     *                      the row number can be provided here, use -1 if inserting a new row
     * @param mappedTerm    the name of the previously mapped term, can be null
     * @param metadata      the metadata associated with the current term (can be
     *                      null or empty)
     */
    @Override
    public void insertOLSResult(String field, Term selectedValue, Term accession,
                                String ontologyShort, String ontologyLong, int modifiedRow, String mappedTerm, List<String> metadata) {
        addItem((accession.getOntologyName().equalsIgnoreCase("ncbitaxon")
                ? new CvParam("NEWT", accession.getTermOBOId().getIdentifier().substring(accession.getTermOBOId().getIdentifier().indexOf(':') + 1), accession.getLabel(), null)
                : new CvParam(accession.getOntologyName().toUpperCase(), accession.getTermOBOId().getIdentifier(), accession.getLabel(), null)));
    }


    @Override
    public Window getWindow() {
        return ((App) App.getInstance()).getMainFrame();
    }

    @Override
    public int getSize() {
        return elements.size();
    }

    @Override
    public Object getElementAt(int index) {
        if (index >= 0 && index < elements.size())
            return elements.get(index);
        else
            return null;
    }

    @Override
    public Object getSelectedItem() {
        return selectedElement;
    }
}

