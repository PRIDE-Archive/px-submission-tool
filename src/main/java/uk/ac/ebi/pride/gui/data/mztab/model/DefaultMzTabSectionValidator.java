package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-01 13:58
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class DefaultMzTabSectionValidator extends MzTabSectionValidator {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMzTabSectionValidator.class);
    // TODO - All Data sections but metadata have a similar validation algorithm ---> REFACTOR IT OUT!!!

    /**
     * This is part of the validation strategy for mzTab metadata section, as we care only about what we require to be
     * present in terms of this section alone.
     *
     * @param mzTabDocument
     * @param metaData
     * @return
     * @throws MzTabSectionValidatorException
     */
    @Override
    public boolean validate(MzTabDocument mzTabDocument, MetaData metaData) throws MzTabSectionValidatorException {
        // Check we have a title
        if (metaData.getTitle() == null) {
            logger.error("Missing title in mzTab metadata section!");
            return false;
        }
        // From the mzTab specification
        // mzTab version MUST always be reported
        if (metaData.getVersion() == null) {
            logger.error("MISSING mzTab Version information");
            return false;
        }
        // mzTab mode MUST always be reported
        if (metaData.getMode() == null) {
            logger.error("MISSING mzTab Mode information");
            return false;
        }
        // mzTab type MUST always be reported
        if (metaData.getType() == null) {
            logger.error("MISSING mzTab Type information");
            return false;
        }
        // description MUST always be reported
        if (metaData.getDescription() == null) {
            logger.error("MISSING mzTab Description information");
            return false;
        }
        // Check for ID_file
        if (metaData.getFileId() == null) {
            // TODO - Revisit this in the future to make sure file ID is not mandatory
            logger.warn("Missing 'file ID' information in mzTab metadata section!");
        }
        // Validate ms-run entries
        for (int msRunIndex :
                metaData.getAvailableMsRunIndexes()) {
            try {
                if (!metaData.getMsRunEntry(msRunIndex).validate()) {
                    logger.error("INVALID metadata ms-run entry with index '" + msRunIndex + "'");
                    return false;
                }
            } catch (ValidationException e) {
                throw new MzTabSectionValidatorException("ERROR while validating ms-run metadata entry with index '"
                        + msRunIndex + "' due to " + e.getMessage());
            }
        }
        // ms-run location MUST always be reported, for this section, it means that there is, at least, one ms-run
        if (metaData.getAvailableMsRunIndexes().size() == 0) {
            logger.error("ms-run location MUST be reported, but it is MISSING in the current mzTab data");
            return false;
        }
        for (int sampleIndex :
                metaData.getAvailableSampleIndexes()) {
            try {
                if (!metaData.getSampleData(sampleIndex).validate()) {
                    logger.error("INVALID metadata sample entry with index '" + sampleIndex + "'");
                    return false;
                }
            } catch (ValidationException e) {
                throw new MzTabSectionValidatorException("ERROR while validating sample metadata entry with index '"
                        + sampleIndex + "' due to " + e.getMessage());
            }
        }
        // Check that protein_search_engine_score[1-n] is present when protein section is
        if (mzTabDocument.getProteinData() != null) {
            if (metaData.getAvailableProteinSearchEngineScoreIndexes().size() == 0) {
                logger.error("Protein section is present, but THERE IS NO PROTEIN SEARCH ENGINE SCORE");
                return false;
            }
        }
        // Validate each protein search engine score
        for (int index :
                metaData.getAvailableProteinSearchEngineScoreIndexes()) {
            if (!metaData.getProteinSearchEngineScore(index).validate()) {
                logger.error("Protein search engine score with index '" + index + "' IS INVALID!");
                return false;
            }
        }
        // Check that peptide_search_engine_score[1-n] is present when peptide section is
        if (mzTabDocument.getPeptideData() != null) {
            if (metaData.getAvailablePeptideSearchEngineScoreIndexes().size() == 0) {
                logger.error("Peptide section is present, but THERE IS NO PEPTIDE SEARCH ENGINE SCORE");
                return false;
            }
        }
        // Validate each peptide search engine score
        for (int index :
                metaData.getAvailablePeptideSearchEngineScoreIndexes()) {
            if (!metaData.getPeptideSearchEngineScore(index).validate()) {
                logger.error("Peptide search engine score with index '" + index + "' IS INVALID!");
                return false;
            }
        }
        // Check that psm_search_engine_score[1-n] is present when PSM section is
        if (mzTabDocument.getPsmData() != null) {
            if (metaData.getAvailablePsmSearchEngineScoreIndexes().size() == 0) {
                logger.error("PSM section is present, but THERE IS NO PSM SEARCH ENGINE SCORE");
                return false;
            }
        }
        // Validate each psm search engine score
        for (int index :
                metaData.getAvailablePsmSearchEngineScoreIndexes()) {
            if (!metaData.getPsmSearchEngineScore(index).validate()) {
                logger.error("PSM search engine score with index '" + index + "' IS INVALID!");
                return false;
            }
        }
        // Check that smallmolecule_search_engine_score[1-n] is present when Small Molecule section is
        if (mzTabDocument.getSmallMoleculeData() != null) {
            if (metaData.getAvailableSmallMoleculeSearchEngineScoreIndexes().size() == 0) {
                logger.error("Small Molecule section is present, but THERE IS NO SMALL MOLECULE SEARCH ENGINE SCORE");
                return false;
            }
        }
        // Validate each small molecule search engine score
        for (int index :
                metaData.getAvailableSmallMoleculeSearchEngineScoreIndexes()) {
            if (!metaData.getSmallMoleculeSearchEngineScore(index).validate()) {
                logger.error("Small molecule search engine score with index '" + index + "' IS INVALID!");
                return false;
            }
        }
        // FixedMod is always reported
        if (metaData.getAvailableFixedModIndexes().size() == 0) {
            logger.error("NO fixed_mod have been reported!");
            return false;
        }
        // Validate each fixed modification
        for (int index :
                metaData.getAvailableFixedModIndexes()) {
            if (!metaData.getFixedMod(index).validate()) {
                logger.error("Fixed modification with index '" + index + "' IS INVALID!");
                return false;
            }
        }
        // VariableMod is always reported
        if (metaData.getAvailableVariableModIndexes().size() == 0) {
            logger.error("NO variable modifications have been reported!");
            return false;
        }
        // Validate each variable modification
        for (int index :
                metaData.getAvailableVariableModIndexes()) {
            if (!metaData.getVariableMod(index).validate()) {
                logger.error("Variable modification with index '" + index + "' IS INVALID!!!");
                return false;
            }
        }
        // Validate study variables
        // Required
        if ((metaData.getType() == MetaData.MzTabType.QUANTIFICATION) && (metaData.getAvailableStudyVariableIndexes().size() == 0)) {
            logger.error("mzTab type QUANTIFICATION but NO STUDY VARIABLES have been provided");
            return false;
        }
        for (int index :
                metaData.getAvailableStudyVariableIndexes()) {
            if (!metaData.getStudyVariable(index).validate(mzTabDocument)) {
                logger.error("Study variable with index '" + index + "' IS INVALID!!!");
                return false;
            }
        }
        // Quantification method
        if ((metaData.getType() == MetaData.MzTabType.QUANTIFICATION) && (metaData.getMode() == MetaData.MzTabMode.COMPLETE)) {
            if (metaData.getQuantificationMethod() == null) {
                logger.error("MISSING quantification method, required when mzTab type QUANTIFICATION, and mode COMPLETE");
                return false;
            }
        }
        if ((metaData.getQuantificationMethod() != null) && (!metaData.getQuantificationMethod().validate())) {
            logger.error("Provided quantification method DOES NOT VALIDATE!");
            return false;
        }
        // Protein, peptide and small molecule quantification units
        if (metaData.getType() == MetaData.MzTabType.QUANTIFICATION) {
            // Validate Protein quantification unit
            if ((mzTabDocument.getProteinData() != null) && (metaData.getProteinQuantificationUnit() == null)) {
                logger.error("MISSING Protein quantification unit information but Protein SECTION IS PRESENT");
                return false;
            }
            if (!metaData.getProteinQuantificationUnit().validate()) {
                logger.error("given Protein quantification unit DOES NOT VALIDATE");
                return false;
            }
            // Validate Peptide quantification unit
            if ((mzTabDocument.getPeptideData() != null) && (metaData.getPeptideQuantificationUnit() == null)) {
                logger.error("MISSING Peptide quantification unit information but Peptide SECTION IS PRESENT");
                return false;
            }
            if (!metaData.getPeptideQuantificationUnit().validate()) {
                logger.error("given Peptide quantification unit DOES NOT VALIDATE");
                return false;
            }
            // Validate Small Molecule quantification unit
            if ((mzTabDocument.getSmallMoleculeData() != null) && (metaData.getSmallMoleculeQuantificationUnit() == null)) {
                logger.error("MISSING Small Molecule quantification unit information but Small Molecule SECTION IS PRESENT");
                return false;
            }
            if (!metaData.getSmallMoleculeQuantificationUnit().validate()) {
                logger.error("given Small Molecule quantification unit DOES NOT VALIDATE");
                return false;
            }
        }
        // Validate Assays, required when quantification and complete
        if ((metaData.getType() == MetaData.MzTabType.QUANTIFICATION) && (metaData.getMode() == MetaData.MzTabMode.COMPLETE)) {
            if (metaData.getAvailableAssayIndexes().size() == 0) {
                logger.error("MISSING Assays information required in mzTab type QUANTIFICATION, mode COMPLETE");
                return false;
            }
        }
        for (int index :
                metaData.getAvailableAssayIndexes()) {
            if (!metaData.getAssay(index).validate(mzTabDocument)) {
                logger.error("Assay with index '" + index + "' DOES NOT VALIDATE");
                return false;
            }
        }
        // Validate Software information, required in mzTab mode COMPLETE
        if (metaData.getMode() == MetaData.MzTabMode.COMPLETE) {
            if (metaData.getAvailableSoftwareEntryIndexes().size() == 0) {
                logger.error("MISSING Software information, required in mzTab mode COMPLETE");
                return false;
            }
        }
        for (int index :
                metaData.getAvailableSoftwareEntryIndexes()) {
            if (!metaData.getSoftware(index).validate(mzTabDocument)) {
                logger.error("Software entry with index '" + index + "' DOES NOT VALIDATE");
                return false;
            }
        }

        // Up to this point, this section validates
        // WARNING - NOTE - Although some of the conditions are tested multiple times, each validation criteria has been
        // implemented redundantly checking on mzTab type or mode on purpose, to ease read of code for future
        // maintainers/contributors, and because it poses no real threat to software performance
        return true;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, ProteinData proteinData) throws MzTabSectionValidatorException {
        // Required columns for mzTab Identification type in Summary mode
        Set<ProteinData.ColumnType> id_sum = new HashSet<>();
        // Required columns for mzTab Identification type in Complete mode
        Set<ProteinData.ColumnType> id_com = new HashSet<>();
        // Required columns for mzTab Quantification type in Summary mode
        Set<ProteinData.ColumnType> qu_sum = new HashSet<>();
        // Required columns for mzTab Quantification type in Complete mode
        Set<ProteinData.ColumnType> qu_com = new HashSet<>();
        // Supported modes
        Set<MetaData.MzTabMode> supportedModes = new HashSet<>(Arrays.asList(MetaData.MzTabMode.COMPLETE, MetaData.MzTabMode.SUMMARY));
        // Supported types
        Set<MetaData.MzTabType> supportedTypes = new HashSet<>(Arrays.asList(MetaData.MzTabType.IDENTIFICATION, MetaData.MzTabType.QUANTIFICATION));
        // Fill in requirements sets
        id_sum.addAll(Arrays.asList(
                ProteinData.ColumnType.ACCESSION, ProteinData.ColumnType.DESCRIPTION, ProteinData.ColumnType.TAXID,
                ProteinData.ColumnType.SPECIES, ProteinData.ColumnType.DATABASE, ProteinData.ColumnType.DATABASE_VERSION,
                ProteinData.ColumnType.SEARCH_ENGINE, ProteinData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                ProteinData.ColumnType.AMBIGUITY_MEMBERS, ProteinData.ColumnType.MODIFICATIONS
        ));
        id_com.addAll(Arrays.asList(
                ProteinData.ColumnType.ACCESSION, ProteinData.ColumnType.DESCRIPTION, ProteinData.ColumnType.TAXID,
                ProteinData.ColumnType.SPECIES, ProteinData.ColumnType.DATABASE, ProteinData.ColumnType.DATABASE_VERSION,
                ProteinData.ColumnType.SEARCH_ENGINE, ProteinData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                ProteinData.ColumnType.AMBIGUITY_MEMBERS, ProteinData.ColumnType.MODIFICATIONS,
                ProteinData.ColumnType.PROTEIN_COVERAGE, ProteinData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN,
                ProteinData.ColumnType.NUM_PSMS_MS_RUN, ProteinData.ColumnType.NUM_PEPTIDES_DISTINCT_MS_RUN,
                ProteinData.ColumnType.NUM_PEPTIDES_UNIQUE_MS_RUN
        ));
        qu_sum.addAll(Arrays.asList(
                ProteinData.ColumnType.ACCESSION, ProteinData.ColumnType.DESCRIPTION, ProteinData.ColumnType.TAXID,
                ProteinData.ColumnType.SPECIES, ProteinData.ColumnType.DATABASE, ProteinData.ColumnType.DATABASE_VERSION,
                ProteinData.ColumnType.SEARCH_ENGINE, ProteinData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                ProteinData.ColumnType.AMBIGUITY_MEMBERS, ProteinData.ColumnType.MODIFICATIONS,
                ProteinData.ColumnType.PROTEIN_ABUNDANCE_STUDY_VARIABLE, ProteinData.ColumnType.PROTEIN_ABUNDANCE_STDEV_STUDY_VARIABLE,
                ProteinData.ColumnType.PROTEIN_ABUNDANCE_STD_ERROR_STUDY_VARIABLE
        ));
        qu_com.addAll(Arrays.asList(
                ProteinData.ColumnType.ACCESSION, ProteinData.ColumnType.DESCRIPTION, ProteinData.ColumnType.TAXID,
                ProteinData.ColumnType.SPECIES, ProteinData.ColumnType.DATABASE, ProteinData.ColumnType.DATABASE_VERSION,
                ProteinData.ColumnType.SEARCH_ENGINE, ProteinData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                ProteinData.ColumnType.AMBIGUITY_MEMBERS, ProteinData.ColumnType.MODIFICATIONS,
                ProteinData.ColumnType.PROTEIN_COVERAGE, ProteinData.ColumnType.PROTEIN_ABUNDANCE_STUDY_VARIABLE,
                ProteinData.ColumnType.PROTEIN_ABUNDANCE_STDEV_STUDY_VARIABLE,
                ProteinData.ColumnType.PROTEIN_ABUNDANCE_STD_ERROR_STUDY_VARIABLE,
                ProteinData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN, ProteinData.ColumnType.PROTEIN_ABUNDANCE_ASSAY
        ));
        // Check for supported mode
        if (!supportedModes.contains(mzTabDocument.getMetaData().getMode())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB MODE '" + mzTabDocument.getMetaData().getMode() + "' IS NOT SUPPORTED");
        }
        // Check for supported type
        if (!supportedTypes.contains(mzTabDocument.getMetaData().getType())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB TYPE '" + mzTabDocument.getMetaData().getType() + "' IS NOT SUPPORTED");
        }
        // Select the testing set
        Set<ProteinData.ColumnType> testingSet = null;
        if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.IDENTIFICATION) {
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // identification type - summary mode
                testingSet = id_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // identification type - complete mode
                testingSet = id_com;
            }
        } else if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.QUANTIFICATION) {
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // quantification type - summary mode
                testingSet = qu_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // quantification type - complete mode
                testingSet = qu_com;
            }
        }
        // Check the required columns
        if (!proteinData.checkThatAllGivenColumnTypesArePresent(testingSet)) {
            logger.error("Protein section IS NOT VALID because of the following missing required columns:");
            for (ProteinData.ColumnType missingColumnType :
                    proteinData.getMissingColumnTypesFromRequiredColumnTypes(testingSet)) {
                logger.error("MISSING Protein Data column '" + missingColumnType.toString() + "' for mzTab "
                        + mzTabDocument.getMetaData().getType().toString() + " Type, "
                        + mzTabDocument.getMetaData().getMode().toString() + " Mode");
            }
            return false;
        }
        // No column interdependencies need to be checked
        return true;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PeptideData peptideData) throws MzTabSectionValidatorException {
        // Required columns for mzTab Identification type in Summary mode
        Set<PeptideData.ColumnType> id_sum = new HashSet<>();
        // Required columns for mzTab Identification type in Complete mode
        Set<PeptideData.ColumnType> id_com = new HashSet<>();
        // Required columns for mzTab Quantification type in Summary mode
        Set<PeptideData.ColumnType> qu_sum = new HashSet<>();
        // Required columns for mzTab Quantification type in Complete mode
        Set<PeptideData.ColumnType> qu_com = new HashSet<>();
        // Supported modes
        Set<MetaData.MzTabMode> supportedModes = new HashSet<>(Arrays.asList(MetaData.MzTabMode.COMPLETE, MetaData.MzTabMode.SUMMARY));
        // Supported types
        Set<MetaData.MzTabType> supportedTypes = new HashSet<>(Arrays.asList(MetaData.MzTabType.IDENTIFICATION, MetaData.MzTabType.QUANTIFICATION));
        // NO COLUMNS are used for identification type, but we don't enforcce columns to be missing
        qu_sum.addAll(Arrays.asList(
                PeptideData.ColumnType.SEQUENCE, PeptideData.ColumnType.ACCESSION, PeptideData.ColumnType.UNIQUE,
                PeptideData.ColumnType.DATABASE, PeptideData.ColumnType.DATABASE_VERSION,
                PeptideData.ColumnType.SEARCH_ENGINE, PeptideData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                PeptideData.ColumnType.MODIFICATIONS, PeptideData.ColumnType.RETENTION_TIME,
                PeptideData.ColumnType.RETENTION_TIME_WINDOW, PeptideData.ColumnType.CHARGE,
                PeptideData.ColumnType.MASS_TO_CHARGE, PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STUDY_VARIABLE,
                PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STDEV_STUDY_VARIABLE,
                PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STD_ERROR_STUDY_VARIABLE
        ));
        qu_com.addAll(Arrays.asList(
                PeptideData.ColumnType.SEQUENCE, PeptideData.ColumnType.ACCESSION, PeptideData.ColumnType.UNIQUE,
                PeptideData.ColumnType.DATABASE, PeptideData.ColumnType.DATABASE_VERSION,
                PeptideData.ColumnType.SEARCH_ENGINE, PeptideData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                PeptideData.ColumnType.MODIFICATIONS, PeptideData.ColumnType.RETENTION_TIME,
                PeptideData.ColumnType.RETENTION_TIME_WINDOW, PeptideData.ColumnType.CHARGE,
                PeptideData.ColumnType.MASS_TO_CHARGE, PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STUDY_VARIABLE,
                PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STDEV_STUDY_VARIABLE,
                PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STD_ERROR_STUDY_VARIABLE,
                PeptideData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN, PeptideData.ColumnType.PEPTIDE_ABUNDANCE_ASSAY,
                PeptideData.ColumnType.SPECTRA_REF
        ));
        // Check for supported mode
        if (!supportedModes.contains(mzTabDocument.getMetaData().getMode())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB MODE '" + mzTabDocument.getMetaData().getMode() + "' IS NOT SUPPORTED");
        }
        // Check for supported type
        if (!supportedTypes.contains(mzTabDocument.getMetaData().getType())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB TYPE '" + mzTabDocument.getMetaData().getType() + "' IS NOT SUPPORTED");
        }
        // Select the testing set
        Set<PeptideData.ColumnType> testingSet = null;
        if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.IDENTIFICATION) {
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // identification type - summary mode
                testingSet = id_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // identification type - complete mode
                testingSet = id_com;
            }
        } else if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.QUANTIFICATION) {
            // TODO Check "spectra_ref" presence if MS2 based quantification is used (NO EXAMPLE OF THIS COULD BE FOUND)
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // quantification type - summary mode
                testingSet = qu_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // quantification type - complete mode
                testingSet = qu_com;
            }
        }
        // Check the required columns
        if (!peptideData.checkThatAllGivenColumnTypesArePresent(testingSet)) {
            logger.error("Peptide section IS NOT VALID because of the following missing required columns:");
            for (PeptideData.ColumnType missingColumnType :
                    peptideData.getMissingColumnTypesFromRequiredColumnTypes(testingSet)) {
                logger.error("MISSING Peptide Data column '" + missingColumnType.toString() + "' for mzTab "
                        + mzTabDocument.getMetaData().getType().toString() + " Type, "
                        + mzTabDocument.getMetaData().getMode().toString() + " Mode");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PsmData psmData) throws MzTabSectionValidatorException {
        // Required columns for mzTab Identification type in Summary mode
        Set<PsmData.ColumnType> id_sum = new HashSet<>();
        // Required columns for mzTab Identification type in Complete mode
        Set<PsmData.ColumnType> id_com = new HashSet<>();
        // Required columns for mzTab Quantification type in Summary mode
        Set<PsmData.ColumnType> qu_sum = new HashSet<>();
        // Required columns for mzTab Quantification type in Complete mode
        Set<PsmData.ColumnType> qu_com = new HashSet<>();
        // Supported modes
        Set<MetaData.MzTabMode> supportedModes = new HashSet<>(Arrays.asList(MetaData.MzTabMode.COMPLETE, MetaData.MzTabMode.SUMMARY));
        // Supported types
        Set<MetaData.MzTabType> supportedTypes = new HashSet<>(Arrays.asList(MetaData.MzTabType.IDENTIFICATION, MetaData.MzTabType.QUANTIFICATION));
        // Fill in the required columns
        id_sum.addAll(Arrays.asList(
                PsmData.ColumnType.SEQUENCE, PsmData.ColumnType.PSM_ID, PsmData.ColumnType.ACCESSION,
                PsmData.ColumnType.UNIQUE, PsmData.ColumnType.DATABASE, PsmData.ColumnType.DATABASE_VERSION,
                PsmData.ColumnType.SEARCH_ENGINE, PsmData.ColumnType.SEARCH_ENGINE_SCORE,
                PsmData.ColumnType.MODIFICATIONS, PsmData.ColumnType.SPECTRA_REF, PsmData.ColumnType.RETENTION_TIME,
                PsmData.ColumnType.CHARGE, PsmData.ColumnType.EXP_MASS_TO_CHARGE,
                PsmData.ColumnType.CALC_MASS_TO_CHARGE, PsmData.ColumnType.PRE, PsmData.ColumnType.POST,
                PsmData.ColumnType.START, PsmData.ColumnType.END
        ));
        id_com.addAll(Arrays.asList(
                PsmData.ColumnType.SEQUENCE, PsmData.ColumnType.PSM_ID, PsmData.ColumnType.ACCESSION,
                PsmData.ColumnType.UNIQUE, PsmData.ColumnType.DATABASE, PsmData.ColumnType.DATABASE_VERSION,
                PsmData.ColumnType.SEARCH_ENGINE, PsmData.ColumnType.SEARCH_ENGINE_SCORE,
                PsmData.ColumnType.MODIFICATIONS, PsmData.ColumnType.SPECTRA_REF, PsmData.ColumnType.RETENTION_TIME,
                PsmData.ColumnType.CHARGE, PsmData.ColumnType.EXP_MASS_TO_CHARGE,
                PsmData.ColumnType.CALC_MASS_TO_CHARGE, PsmData.ColumnType.PRE, PsmData.ColumnType.POST,
                PsmData.ColumnType.START, PsmData.ColumnType.END
        ));
        qu_sum.addAll(Arrays.asList(
                PsmData.ColumnType.SEQUENCE, PsmData.ColumnType.PSM_ID, PsmData.ColumnType.ACCESSION,
                PsmData.ColumnType.UNIQUE, PsmData.ColumnType.DATABASE, PsmData.ColumnType.DATABASE_VERSION,
                PsmData.ColumnType.SEARCH_ENGINE, PsmData.ColumnType.SEARCH_ENGINE_SCORE,
                PsmData.ColumnType.MODIFICATIONS, PsmData.ColumnType.SPECTRA_REF, PsmData.ColumnType.RETENTION_TIME,
                PsmData.ColumnType.CHARGE, PsmData.ColumnType.EXP_MASS_TO_CHARGE,
                PsmData.ColumnType.CALC_MASS_TO_CHARGE, PsmData.ColumnType.PRE, PsmData.ColumnType.POST,
                PsmData.ColumnType.START, PsmData.ColumnType.END
        ));
        qu_com.addAll(Arrays.asList(
                PsmData.ColumnType.SEQUENCE, PsmData.ColumnType.PSM_ID, PsmData.ColumnType.ACCESSION,
                PsmData.ColumnType.UNIQUE, PsmData.ColumnType.DATABASE, PsmData.ColumnType.DATABASE_VERSION,
                PsmData.ColumnType.SEARCH_ENGINE, PsmData.ColumnType.SEARCH_ENGINE_SCORE,
                PsmData.ColumnType.MODIFICATIONS, PsmData.ColumnType.SPECTRA_REF, PsmData.ColumnType.RETENTION_TIME,
                PsmData.ColumnType.CHARGE, PsmData.ColumnType.EXP_MASS_TO_CHARGE,
                PsmData.ColumnType.CALC_MASS_TO_CHARGE, PsmData.ColumnType.PRE, PsmData.ColumnType.POST,
                PsmData.ColumnType.START, PsmData.ColumnType.END
        ));
        // Check for supported mode
        if (!supportedModes.contains(mzTabDocument.getMetaData().getMode())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB MODE '" + mzTabDocument.getMetaData().getMode() + "' IS NOT SUPPORTED");
        }
        // Check for supported type
        if (!supportedTypes.contains(mzTabDocument.getMetaData().getType())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB TYPE '" + mzTabDocument.getMetaData().getType() + "' IS NOT SUPPORTED");
        }
        // Select the testing set
        Set<PsmData.ColumnType> testingSet = null;
        if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.IDENTIFICATION) {
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // identification type - summary mode
                testingSet = id_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // identification type - complete mode
                testingSet = id_com;
            }
        } else if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.QUANTIFICATION) {
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // quantification type - summary mode
                testingSet = qu_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // quantification type - complete mode
                testingSet = qu_com;
            }
        }
        // Check the required columns
        if (!psmData.checkThatAllGivenColumnTypesArePresent(testingSet)) {
            logger.error("PSM section IS NOT VALID because of the following missing required columns:");
            for (PsmData.ColumnType missingColumnType :
                    psmData.getMissingColumnTypesFromRequiredColumnTypes(testingSet)) {
                logger.error("MISSING PSM Data column '" + missingColumnType.toString() + "' for mzTab "
                        + mzTabDocument.getMetaData().getType().toString() + " Type, "
                        + mzTabDocument.getMetaData().getMode().toString() + " Mode");
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, SmallMoleculeData smallMoleculeData) throws MzTabSectionValidatorException {
        // Required columns for mzTab Identification type in Summary mode
        Set<SmallMoleculeData.ColumnType> id_sum = new HashSet<>();
        // Required columns for mzTab Identification type in Complete mode
        Set<SmallMoleculeData.ColumnType> id_com = new HashSet<>();
        // Required columns for mzTab Quantification type in Summary mode
        Set<SmallMoleculeData.ColumnType> qu_sum = new HashSet<>();
        // Required columns for mzTab Quantification type in Complete mode
        Set<SmallMoleculeData.ColumnType> qu_com = new HashSet<>();
        // Supported modes
        Set<MetaData.MzTabMode> supportedModes = new HashSet<>(Arrays.asList(MetaData.MzTabMode.COMPLETE, MetaData.MzTabMode.SUMMARY));
        // Supported types
        Set<MetaData.MzTabType> supportedTypes = new HashSet<>(Arrays.asList(MetaData.MzTabType.IDENTIFICATION, MetaData.MzTabType.QUANTIFICATION));
        // Fill in the required columns
        id_sum.addAll(Arrays.asList(
                SmallMoleculeData.ColumnType.IDENTIFIER, SmallMoleculeData.ColumnType.CHEMICAL_FORMULA,
                SmallMoleculeData.ColumnType.SMILES, SmallMoleculeData.ColumnType.INCHI_KEY,
                SmallMoleculeData.ColumnType.DESCRIPTION, SmallMoleculeData.ColumnType.EXP_MASS_TO_CHARGE,
                SmallMoleculeData.ColumnType.CALC_MASS_TO_CHARGE, SmallMoleculeData.ColumnType.CHARGE,
                SmallMoleculeData.ColumnType.RETENTION_TIME, SmallMoleculeData.ColumnType.TAXID,
                SmallMoleculeData.ColumnType.SPECIES, SmallMoleculeData.ColumnType.DATABASE,
                SmallMoleculeData.ColumnType.DATABASE_VERSION, SmallMoleculeData.ColumnType.SPECTRA_REF,
                SmallMoleculeData.ColumnType.SEARCH_ENGINE, SmallMoleculeData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                SmallMoleculeData.ColumnType.MODIFICATIONS
        ));
        id_com.addAll(Arrays.asList(
                SmallMoleculeData.ColumnType.IDENTIFIER, SmallMoleculeData.ColumnType.CHEMICAL_FORMULA,
                SmallMoleculeData.ColumnType.SMILES, SmallMoleculeData.ColumnType.INCHI_KEY,
                SmallMoleculeData.ColumnType.DESCRIPTION, SmallMoleculeData.ColumnType.EXP_MASS_TO_CHARGE,
                SmallMoleculeData.ColumnType.CALC_MASS_TO_CHARGE, SmallMoleculeData.ColumnType.CHARGE,
                SmallMoleculeData.ColumnType.RETENTION_TIME, SmallMoleculeData.ColumnType.TAXID,
                SmallMoleculeData.ColumnType.SPECIES, SmallMoleculeData.ColumnType.DATABASE,
                SmallMoleculeData.ColumnType.DATABASE_VERSION, SmallMoleculeData.ColumnType.SPECTRA_REF,
                SmallMoleculeData.ColumnType.SEARCH_ENGINE, SmallMoleculeData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                SmallMoleculeData.ColumnType.MODIFICATIONS
        ));
        qu_sum.addAll(Arrays.asList(
                SmallMoleculeData.ColumnType.IDENTIFIER, SmallMoleculeData.ColumnType.CHEMICAL_FORMULA,
                SmallMoleculeData.ColumnType.SMILES, SmallMoleculeData.ColumnType.INCHI_KEY,
                SmallMoleculeData.ColumnType.DESCRIPTION, SmallMoleculeData.ColumnType.EXP_MASS_TO_CHARGE,
                SmallMoleculeData.ColumnType.CALC_MASS_TO_CHARGE, SmallMoleculeData.ColumnType.CHARGE,
                SmallMoleculeData.ColumnType.RETENTION_TIME, SmallMoleculeData.ColumnType.TAXID,
                SmallMoleculeData.ColumnType.SPECIES, SmallMoleculeData.ColumnType.DATABASE,
                SmallMoleculeData.ColumnType.DATABASE_VERSION, SmallMoleculeData.ColumnType.SPECTRA_REF,
                SmallMoleculeData.ColumnType.SEARCH_ENGINE, SmallMoleculeData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                SmallMoleculeData.ColumnType.MODIFICATIONS
        ));
        qu_com.addAll(Arrays.asList(
                SmallMoleculeData.ColumnType.IDENTIFIER, SmallMoleculeData.ColumnType.CHEMICAL_FORMULA,
                SmallMoleculeData.ColumnType.SMILES, SmallMoleculeData.ColumnType.INCHI_KEY,
                SmallMoleculeData.ColumnType.DESCRIPTION, SmallMoleculeData.ColumnType.EXP_MASS_TO_CHARGE,
                SmallMoleculeData.ColumnType.CALC_MASS_TO_CHARGE, SmallMoleculeData.ColumnType.CHARGE,
                SmallMoleculeData.ColumnType.RETENTION_TIME, SmallMoleculeData.ColumnType.TAXID,
                SmallMoleculeData.ColumnType.SPECIES, SmallMoleculeData.ColumnType.DATABASE,
                SmallMoleculeData.ColumnType.DATABASE_VERSION, SmallMoleculeData.ColumnType.SPECTRA_REF,
                SmallMoleculeData.ColumnType.SEARCH_ENGINE, SmallMoleculeData.ColumnType.BEST_SEARCH_ENGINE_SCORE,
                SmallMoleculeData.ColumnType.MODIFICATIONS, SmallMoleculeData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN
        ));
        // Check for supported mode
        if (!supportedModes.contains(mzTabDocument.getMetaData().getMode())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB MODE '" + mzTabDocument.getMetaData().getMode() + "' IS NOT SUPPORTED");
        }
        // Check for supported type
        if (!supportedTypes.contains(mzTabDocument.getMetaData().getType())) {
            throw new MzTabSectionValidatorException("VALIDATION OF MZTAB TYPE '" + mzTabDocument.getMetaData().getType() + "' IS NOT SUPPORTED");
        }
        // Select the testing set
        Set<SmallMoleculeData.ColumnType> testingSet = null;
        if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.IDENTIFICATION) {
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // identification type - summary mode
                testingSet = id_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // identification type - complete mode
                testingSet = id_com;
            }
        } else if (mzTabDocument.getMetaData().getType() == MetaData.MzTabType.QUANTIFICATION) {
            // Check smallmolecule_abundance_assay[1-n] if assays reported
            if (mzTabDocument.getMetaData().getAvailableAssayIndexes().size() > 0) {
                testingSet.add(SmallMoleculeData.ColumnType.SMALLMOLECULE_ABUNDANCE_ASSAY);
            }
            // If study vars reported
            if (mzTabDocument.getMetaData().getAvailableStudyVariableIndexes().size() > 0) {
                // Check smallmolecule_abundance_study_variable[1-n]
                testingSet.add(SmallMoleculeData.ColumnType.SMALLMOLECULE_ABUNDANCE_STUDY_VARIABLE);
                // Check smallmolecule_stdev_study_variable[1-n]
                testingSet.add(SmallMoleculeData.ColumnType.SMALLMOLECULE_STDEV_STUDY_VARIABLE);
                // Check smallmolecule_std_error_study_variable[1-n]
                testingSet.add(SmallMoleculeData.ColumnType.SMALLMOLECULE_STD_ERROR_STUDY_VARIABLE);
            }
            if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.SUMMARY) {
                // quantification type - summary mode
                testingSet = qu_sum;
            } else if (mzTabDocument.getMetaData().getMode() == MetaData.MzTabMode.COMPLETE) {
                // quantification type - complete mode
                testingSet = qu_com;
            }
        }
        // Check the required columns
        if (!smallMoleculeData.checkThatAllGivenColumnTypesArePresent(testingSet)) {
            logger.error("Small Molecule section IS NOT VALID because of the following missing required columns:");
            for (SmallMoleculeData.ColumnType missingColumnType :
                    smallMoleculeData.getMissingColumnTypesFromRequiredColumnTypes(testingSet)) {
                logger.error("MISSING Small Molecule Data column '" + missingColumnType.toString() + "' for mzTab "
                        + mzTabDocument.getMetaData().getType().toString() + " Type, "
                        + mzTabDocument.getMetaData().getMode().toString() + " Mode");
            }
            return false;
        }
        return true;
    }
}
