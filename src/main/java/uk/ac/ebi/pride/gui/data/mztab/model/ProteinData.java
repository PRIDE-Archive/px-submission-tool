package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidProteinSection;

import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-06-27 15:02
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class manages Protein Section data in an mzTab document
 */

public class ProteinData {
    private static final Logger logger = LoggerFactory.getLogger(ProteinData.class);

    // NOTE - This implementation of mzTab parser embedded in the px-submission-tool IS NOT a complete parser, it only
    // deals with the mzTab data that is of interest for the submission process, that's why we don't need to hold any
    // more information other than the presence/absence of a column for quick and lightweight validation purposes
    public enum ColumnType {
        ACCESSION,
        DESCRIPTION,
        TAXID,
        SPECIES,
        DATABASE,
        DATABASE_VERSION,
        SEARCH_ENGINE,
        BEST_SEARCH_ENGINE_SCORE,
        AMBIGUITY_MEMBERS,
        MODIFICATIONS,
        PROTEIN_COVERAGE,
        PROTEIN_ABUNDANCE_STUDY_VARIABLE,
        PROTEIN_ABUNDANCE_STDEV_STUDY_VARIABLE,
        PROTEIN_ABUNDANCE_STD_ERROR_STUDY_VARIABLE,
        SEARCH_ENGINE_SCORE_MS_RUN,
        NUM_PSMS_MS_RUN,
        NUM_PEPTIDES_DISTINCT_MS_RUN,
        NUM_PEPTIDE_UNIQUE_MS_RUN,
        PROTEIN_ABUNDANCE_ASSAY,
        OPT_CUSTOM_ATTIBUTE,
        GO_TERMS,
        RELIABILITY,
        URI
    }

    // Present columns
    private Set<ColumnType> columnsFound = new HashSet<>();

    /**
     * Report a column present at the given index
     *
     * @param index      where the reported column type has been found
     * @param columnType column type found
     */
    public void addColumn(int index, ColumnType columnType) {
        // NOTE - We ignore the index, as we don't need it at this stage
        columnsFound.add(columnType);
    }

    /**
     * Returns true if a particular column type has been reported, at least once
     * @param columnType to look for
     * @return true if a particular column type has been reported, false otherwise
     */
    public boolean isColumnTypePresent(ColumnType columnType) {
        return columnsFound.contains(columnType);
    }

    /**
     * Validation algorithm for the protein section is, in part, coupled with the metadata section that describes the
     * mzTab document it belongs to.
     *
     * @param metaData Metadata section that describes the mzTab document where this protein section belongs to
     * @throws InvalidProteinSection thrown if this protein section is not valid
     */
    public void validate(MzTabDocument mzTabDocument) throws InvalidProteinSection {
        // TODO
    }
}
