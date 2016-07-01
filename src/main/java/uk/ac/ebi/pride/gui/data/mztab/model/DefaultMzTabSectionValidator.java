package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (metaData.getVersion() == null) {
            logger.error("Missing version information in mzTab metadata section!");
            return false;
        }
        if (metaData.getType() == null) {
            logger.error("Missing 'type' information in mzTab metadata section!");
            return false;
        }
        if (metaData.getFileId() == null) {
            logger.error("Missin 'file ID' information in mzTab metadata section!");
            return false;
        }
        if (metaData.getMode() == null) {
            logger.error("NO mzTab MODE has been specified!");
            return false;
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
        if (metaData.getDescription() == null) {
            logger.warn("No 'description' information for the parsed mzTab document");
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
        // Up to this point, this section validates
        return true;
    }

    @Override
    public boolean validate(MzTabDocument mzTabDocument, ProteinData proteinData) throws MzTabSectionValidatorException {
        // TODO
       return false;
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
