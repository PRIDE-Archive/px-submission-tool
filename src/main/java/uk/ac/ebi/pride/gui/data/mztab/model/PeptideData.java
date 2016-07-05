package uk.ac.ebi.pride.gui.data.mztab.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-06-27 16:05
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class manages Peptide data in an mzTab document
 */

public class PeptideData implements MzTabSection {

    public enum ColumnType {
        SEQUENCE,
        ACCESSION,
        UNIQUE,
        DATABASE,
        DATABASE_VERSION,
        SEARCH_ENGINE,
        BEST_SEARCH_ENGINE_SCORE,
        MODIFICATIONS,
        RETENTION_TIME,
        RETENTION_TIME_WINDOW,
        CHARGE,
        MASS_TO_CHARGE,
        PEPTIDE_ABUNDANCE_STUDY_VARIABLE,
        PEPTIDE_ABUNDANCE_STDEV_STUDY_VARIABLE,
        PEPTIDE_ABUNDANCE_STD_ERROR_STUDY_VARIABLE,
        SEARCH_ENGINE_SCORE_MS_RUN,
        PEPTIDE_ABUNDANCE_ASSAY,
        SPECTRA_REF,
        OPT_CUSTOM_ATTIBUTE,
        RELIABILITY,
        URI
    }

    // Present columns
    private Set<PeptideData.ColumnType> columnsFound = new HashSet<>();

    /**
     * Report a column present at the given index
     *
     * @param index      where the reported column type has been found
     * @param columnType column type found
     */
    public void addColumn(int index, PeptideData.ColumnType columnType) {
        // NOTE - We ignore the index, as we don't need it at this stage
        columnsFound.add(columnType);
    }

    /**
     * Returns true if a particular column type has been reported, at least once
     * @param columnType to look for
     * @return true if a particular column type has been reported, false otherwise
     */
    public boolean isColumnTypePresent(PeptideData.ColumnType columnType) {
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
            throw new InvalidMzTabSectionException("An ERROR occurred while validating Peptide mzTab section: " + e.getMessage());
        }
    }
}
