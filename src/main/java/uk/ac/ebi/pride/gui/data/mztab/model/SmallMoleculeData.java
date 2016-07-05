package uk.ac.ebi.pride.gui.data.mztab.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-06-27 16:23
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class manages Small Molecule data for an mzTab document
 */

public class SmallMoleculeData implements MzTabSection {

    public enum ColumnType {
        IDENTIFIER("identifier"),
        CHEMICAL_FORMULA("chemical_formula"),
        SMILES("smiles"),
        INCHI_KEY("inchi_key"),
        DESCRIPTION("description"),
        EXP_MASS_TO_CHARGE("exp_mass_to_charge"),
        CALC_MASS_TO_CHARGE("calc_mass_to_charge"),
        CHARGE("charge"),
        RETENTION_TIME("retention_time"),
        TAXID("taxid"),
        SPECIES("species"),
        DATABASE("database"),
        DATABASE_VERSION("database_version"),
        SPECTRA_REF("spectra_ref"),
        SEARCH_ENGINE("search_engine"),
        BEST_SEARCH_ENGINE_SCORE("best_search_engine_score[1-n]"),
        MODIFICATIONS("modifications"),
        SMALLMOLECULE_ABUNDANCE_ASSAY("smallmolecule_abundance_assay[1-n]"),
        SMALLMOLECULE_ABUNDANCE_STUDY_VARIABLE("smallmolecule_abundance_study_variable[1-n]"),
        SMALLMOLECULE_STDEV_STUDY_VARIABLE("smallmolecule_stdev_study_variable[1-n]"),
        SMALLMOLECULE_STD_ERROR_STUDY_VARIABLE("smallmolecule_std_error_study_variable[1-n]"),
        SEARCH_ENGINE_SCORE_MS_RUN("search_engine_score[1-n]_ms_run[1-n]"),
        OPT_CUSTOM_ATTRIBUTE("opt_{identifier}_*"),
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
    private Set<SmallMoleculeData.ColumnType> columnsFound = new HashSet<>();

    /**
     * Report a column present at the given index
     *
     * @param index      where the reported column type has been found
     * @param columnType column type found
     */
    public void addColumn(int index, SmallMoleculeData.ColumnType columnType) {
        // NOTE - We ignore the index, as we don't need it at this stage
        columnsFound.add(columnType);
    }

    /**
     * Returns true if a particular column type has been reported, at least once
     * @param columnType to look for
     * @return true if a particular column type has been reported, false otherwise
     */
    public boolean isColumnTypePresent(SmallMoleculeData.ColumnType columnType) {
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
            throw new InvalidMzTabSectionException("An ERROR occurred while validating Small Molecule mzTab section: " + e.getMessage());
        }
    }
}
