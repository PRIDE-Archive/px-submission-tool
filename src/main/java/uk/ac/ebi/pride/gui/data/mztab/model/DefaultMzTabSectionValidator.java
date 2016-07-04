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

    /**
     * This is part of the validation strategy for mzTab metadata section, as we care only about what we require to be
     * present in terms of this section alone.
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
            logger.warn("Missin 'file ID' information in mzTab metadata section!");
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
        // TODO Cross-check *_search_engine_score with their corresponding sections
        // TODO fixed_mod and variable_mod MUST be reported
        // Up to this point, this section validates
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
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, PsmData psmData) throws MzTabSectionValidatorException {
        // TODO
        return false;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, SmallMoleculeData smallMoleculeData) throws MzTabSectionValidatorException {
        // TODO
        return false;
    }
}
