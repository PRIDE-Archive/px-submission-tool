package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-06-27 15:02
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * This class manages Protein Section data in an mzTab document
 */

public class ProteinData implements MzTabSection {
    private static final Logger logger = LoggerFactory.getLogger(ProteinData.class);

    // NOTE - This implementation of mzTab parser embedded in the px-submission-tool IS NOT a complete parser, it only
    // deals with the mzTab data that is of interest for the submission process, that's why we don't need to hold any
    // more information other than the presence/absence of a column for quick and lightweight validation purposes
    public enum ColumnType {
        ACCESSION("accession"),
        DESCRIPTION("description"),
        TAXID("taxid"),
        SPECIES("species"),
        DATABASE("database"),
        DATABASE_VERSION("database_version"),
        SEARCH_ENGINE("search_engine"),
        BEST_SEARCH_ENGINE_SCORE("best_search_engine_score[1-n]"),
        AMBIGUITY_MEMBERS("ambiguity_members"),
        MODIFICATIONS("modifications"),
        PROTEIN_COVERAGE("protein_coverage"),
        PROTEIN_ABUNDANCE_STUDY_VARIABLE("protein_abundance_study_variable[1-n]"),
        PROTEIN_ABUNDANCE_STDEV_STUDY_VARIABLE("protein_abundance_stdev_study_variable[1-n]"),
        PROTEIN_ABUNDANCE_STD_ERROR_STUDY_VARIABLE("protein_abundance_std_error_study_variable[1-n]"),
        SEARCH_ENGINE_SCORE_MS_RUN("search_engine_score[1-n]_ms_run[1-n]"),
        NUM_PSMS_MS_RUN("num_psms_ms_run[1-n]"),
        NUM_PEPTIDES_DISTINCT_MS_RUN("num_peptides_distinct_ms_run[1-n]"),
        NUM_PEPTIDES_UNIQUE_MS_RUN("num_peptides_unique_ms_run[1-n]"),
        PROTEIN_ABUNDANCE_ASSAY("protein_abundance_assay[1-n]"),
        OPT_CUSTOM_ATTIBUTE("opt_{identifier}"),
        GO_TERMS("go_terms"),
        RELIABILITY("reliability"),
        URI("uri");

        private String colTypeToString = "";

        ColumnType(String colTypeToString) {
            this.colTypeToString = colTypeToString;
        }


        @Override
        public String toString() {
            return colTypeToString;
        }
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
     *
     * @param columnType to look for
     * @return true if a particular column type has been reported, false otherwise
     */
    public boolean isColumnTypePresent(ColumnType columnType) {
        return columnsFound.contains(columnType);
    }

    public int getNumberOfColumns() {
        return columnsFound.size();
    }

    public boolean hasHeaderBeenSpecified() {
        return (getNumberOfColumns() != 0);
    }

    /**
     * Check that a set of column types are ALL present in this section
     * @param columnTypes set of required column types
     * @return true if they are all present, false otherwise
     */
    public boolean checkThatAllGivenColumnTypesArePresent(Set<ColumnType> columnTypes) {
        return columnsFound.containsAll(columnTypes);
    }

    /**
     * For a given set of column types, calculate which of them are missing in this section
     * @param columnTypes set of column types
     * @return a set of missing column types among the given ones in this section, empty set if they're all present
     */
    public Set<ColumnType> getMissingColumnTypesFromRequiredColumnTypes(Set<ColumnType> columnTypes) {
        Set<ColumnType> result = new HashSet<>(columnTypes);
        result.removeAll(columnsFound);
        return result;
    }

    @Override

    public boolean validate(MzTabDocument mzTabDocument, MzTabSectionValidator validator) throws InvalidMzTabSectionException {
        try {
            return validator.validate(mzTabDocument, this);
        } catch (MzTabSectionValidatorException e) {
            throw new InvalidMzTabSectionException("An ERROR occurred while validating Protein mzTab section: " + e.getMessage());
        }
    }
}
