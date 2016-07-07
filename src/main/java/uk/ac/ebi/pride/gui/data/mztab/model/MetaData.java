package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 09:28
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class MetaData implements MzTabSection {
    private static final Logger logger = LoggerFactory.getLogger(MetaData.class);

    // mzTab modes
    public enum MzTabType {
        QUANTIFICATION("Quantification"),
        IDENTIFICATION("Identification");

        private String type;

        MzTabType(String type) {
            this.type = type;
        }

        public String getValue() {
            return toString();
        }

        @Override
        public String toString() {
            return type;
        }
    }

    // mzTab types
    public enum MzTabMode {
        COMPLETE("Complete"),
        SUMMARY("Summary");

        private String mode;

        MzTabMode(String mode) {
            this.mode = mode;
        }

        public String getValue() {
            return toString();
        }

        @Override
        public String toString() {
            return mode;
        }
    }

    // Bean fields
    // mzTab-version
    private String version;
    // mzTab-mode
    private MzTabMode mode;
    // mzTab-type
    private MzTabType type;
    // mzTab-ID
    private String fileId;
    // title
    private String title;
    // description
    private String description;
    // ms-run entries
    private Map<Integer, MsRun> msRuns;
    // samples
    private Map<Integer, Sample> samples;
    // indexed protein_search_engine_score
    // TODO - NOTE - it says on the documentation that they must be reported for every search engine score reported in the corresponding section
    private Map<Integer, ProteinSearchEngineScore> proteinSearchEngineScores;
    // indexed peptide_search_engine_score
    // TODO - NOTE - it says on the documentation that they must be reported for every search engine score reported in the corresponding section
    private Map<Integer, PeptideSearchEngineScore> peptideSearchEngineScores;
    // indexed psm_search_engine_score
    // TODO - NOTE - it says on the documentation that they must be reported for every search engine score reported in the corresponding section
    private Map<Integer, PsmSearchEngineScore> psmSearchEngineScores;
    // indexed small molecule_search_engine_score
    // TODO - NOTE - it says on the documentation that they must be reported for every search engine score reported in the corresponding section
    private Map<Integer, SmallMoleculeSearchEngineScore> smallMoleculeSearchEngineScores;
    // Fixed modification entries
    private Map<Integer, FixedMod> fixedMods;
    // Variable modification entries
    private Map<Integer, VariableMod> variableMods;
    private QuantificationMethod quantificationMethod;
    private ProteinQuantificationUnit proteinQuantificationUnit;
    private PeptideQuantificationUnit peptideQuantificationUnit;
    private SmallMoleculeQuantificationUnit smallMoleculeQuantificationUnit;
    // assay entries
    private Map<Integer, Assay> assays;
    // study_variable entries
    private Map<Integer, StudyVariable> studyVariables;
    private Map<Integer, Software> softwareEntries;

    public MetaData() {
        version = null;
        mode = null;
        type = null;
        fileId = null;
        title = null;
        description = null;
        msRuns = new HashMap<>();
        samples = new HashMap<>();
        proteinSearchEngineScores = new HashMap<>();
        peptideSearchEngineScores = new HashMap<>();
        psmSearchEngineScores = new HashMap<>();
        smallMoleculeSearchEngineScores = new HashMap<>();
        fixedMods = new HashMap<>();
        variableMods = new HashMap<>();
        assays = new HashMap<>();
        studyVariables = new HashMap<>();
        quantificationMethod = null;
        proteinQuantificationUnit = null;
        peptideQuantificationUnit = null;
        smallMoleculeQuantificationUnit = null;
        softwareEntries = new HashMap<>();
    }

    // Getters/Setters
    public String getVersion() {
        return version;
    }

    public MzTabMode getMode() {
        return mode;
    }

    public MzTabType getType() {
        return type;
    }

    public String getFileId() {
        return fileId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setVersion(String version) {

        this.version = version;
    }

    public void setMode(MzTabMode mode) {
        this.mode = mode;
    }

    public void setType(MzTabType type) {
        this.type = type;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuantificationMethod getQuantificationMethod() {
        return quantificationMethod;
    }

    public void setQuantificationMethod(QuantificationMethod quantificationMethod) {
        this.quantificationMethod = quantificationMethod;
    }

    public ProteinQuantificationUnit getProteinQuantificationUnit() {
        return proteinQuantificationUnit;
    }

    public void setProteinQuantificationUnit(ProteinQuantificationUnit proteinQuantificationUnit) {
        this.proteinQuantificationUnit = proteinQuantificationUnit;
    }

    public PeptideQuantificationUnit getPeptideQuantificationUnit() {
        return peptideQuantificationUnit;
    }

    public void setPeptideQuantificationUnit(PeptideQuantificationUnit peptideQuantificationUnit) {
        this.peptideQuantificationUnit = peptideQuantificationUnit;
    }

    public SmallMoleculeQuantificationUnit getSmallMoleculeQuantificationUnit() {
        return smallMoleculeQuantificationUnit;
    }

    public void setSmallMoleculeQuantificationUnit(SmallMoleculeQuantificationUnit smallMoleculeQuantificationUnit) {
        this.smallMoleculeQuantificationUnit = smallMoleculeQuantificationUnit;
    }

    // MsRun Entries management
    public MsRun updateMsRun(MsRun msRun, int index) {
        logger.debug("Adding ms-run with index " + index);
        return msRuns.put(index, msRun);
    }

    public MsRun getMsRunEntry(int index) {
        return msRuns.get(index);
    }

    public Set<Integer> getAvailableMsRunIndexes() {
        return msRuns.keySet();
    }

    // Sample Entries management
    public void updateSampleData(Sample sample, int index) {
        logger.debug("Adding sample data entry, index " + index);
        samples.put(index, sample);
    }

    public Sample getSampleData(int index) {
        return samples.get(index);
    }

    public Set<Integer> getAvailableSampleIndexes() {
        return samples.keySet();
    }

    // Protein search engine score management
    public ProteinSearchEngineScore updateProteinSearchEngineScore(ProteinSearchEngineScore proteinSearchEngineScore, int index) {
        return proteinSearchEngineScores.put(index, proteinSearchEngineScore);
    }

    public ProteinSearchEngineScore getProteinSearchEngineScore(int index) {
        return proteinSearchEngineScores.get(index);
    }

    public Set<Integer> getAvailableProteinSearchEngineScoreIndexes() {
        return proteinSearchEngineScores.keySet();
    }

    // Peptide search engine score management
    public PeptideSearchEngineScore updatePeptideSearchEngineScore(PeptideSearchEngineScore peptideSearchEngineScore, int index) {
        return peptideSearchEngineScores.put(index, peptideSearchEngineScore);
    }

    public PeptideSearchEngineScore getPeptideSearchEngineScore(int index) {
        return peptideSearchEngineScores.get(index);
    }

    public Set<Integer> getAvailablePeptideSearchEngineScoreIndexes() {
        return peptideSearchEngineScores.keySet();
    }

    // PSM search engine score management
    public PsmSearchEngineScore updatePsmSearchEngineScore(PsmSearchEngineScore psmSearchEngineScore, int index) {
        return psmSearchEngineScores.put(index, psmSearchEngineScore);
    }

    public PsmSearchEngineScore getPsmSearchEngineScore(int index) {
        return psmSearchEngineScores.get(index);
    }

    public Set<Integer> getAvailablePsmSearchEngineScoreIndexes() {
        return psmSearchEngineScores.keySet();
    }

    // Small Molecule search engine score management
    public SmallMoleculeSearchEngineScore updateSmallMoleculeSearchEngineScore(SmallMoleculeSearchEngineScore smallMoleculeSearchEngineScore, int index) {
        return smallMoleculeSearchEngineScores.put(index, smallMoleculeSearchEngineScore);
    }

    public SmallMoleculeSearchEngineScore getSmallMoleculeSearchEngineScore(int index) {
        return smallMoleculeSearchEngineScores.get(index);
    }

    public Set<Integer> getAvailableSmallMoleculeSearchEngineScoreIndexes() {
        return smallMoleculeSearchEngineScores.keySet();
    }

    // Fixed modifications management
    public FixedMod updateFixedMod(FixedMod fixedMod, int index) {
        return fixedMods.put(index, fixedMod);
    }

    public FixedMod getFixedMod(int index) {
        return fixedMods.get(index);
    }

    public Set<Integer> getAvailableFixedModIndexes() {
        return fixedMods.keySet();
    }

    // Variable modifications management
    public VariableMod updateVariableMod(VariableMod variableMod, int index) {
        return variableMods.put(index, variableMod);
    }

    public VariableMod getVariableMod(int index) {
        return variableMods.get(index);
    }

    public Set<Integer> getAvailableVariableModIndexes() {
        return variableMods.keySet();
    }

    // Assays management
    public Assay updateAssay(Assay assay, int index) {
        return assays.put(index, assay);
    }

    public Assay getAssay(int index) {
        return assays.get(index);
    }

    public Set<Integer> getAvailableAssayIndexes() {
        return assays.keySet();
    }

    // Study variable entries management
    public StudyVariable updateStudyVariable(StudyVariable studyVariable, int index) {
        return studyVariables.put(index, studyVariable);
    }

    public StudyVariable getStudyVariable(int index) {
        return studyVariables.get(index);
    }

    public Set<Integer> getAvailableStudyVariableIndexes() {
        return studyVariables.keySet();
    }

    // Software entries management
    public Software updateSoftware(Software software, int index) {
        return softwareEntries.put(index, software);
    }

    public Software getSoftware(int index) {
        return softwareEntries.get(index);
    }

    public Set<Integer> getAvailableSoftwareEntryIndexes() {
        return softwareEntries.keySet();
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, MzTabSectionValidator validator) throws InvalidMzTabSectionException {
        try {
            return validator.validate(mzTabDocument, this);
        } catch (MzTabSectionValidatorException e) {
            throw new InvalidMzTabSectionException("An ERROR occurred while validating MetaData mzTab section: " + e.getMessage());
        }
    }
}
