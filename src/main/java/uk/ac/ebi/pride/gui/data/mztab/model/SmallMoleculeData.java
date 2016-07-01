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

public class SmallMoleculeData extends MzTabSection {

    public enum ColumnType {
        IDENTIFIER,
        CHEMICAL_FORMULA,
        SMILES,
        INCHI_KEY,
        DESCRIPTION,
        EXP_MASS_TO_CHARGE,
        CALC_MASS_TO_CHARGE,
        CHARGE,
        RETENTION_TIME,
        TAXID,
        SPECIES,
        DATABASE,
        DATABASE_VERSION,
        SPECTRA_REF,
        SEARCH_ENGINE,
        BEST_SEARCH_ENGINE_SCORE,
        MODIFICATIONS,
        SMALLMOLECULE_ABUNDANCE_ASSAY,
        SMALLMOLECULE_ABUNDANCE_STUDY_VARIABLE,
        SMALLMOLECULE_STDEV_STUDY_VARIABLE,
        SMALLMOLECULE_STD_ERROR_STUDY_VARIABLE,
        SEARCH_ENGINE_SCORE_MS_RUN,
        OPT_CUSTOM_ATTRIBUTE,
        RELIABILITY,
        URI
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

}
