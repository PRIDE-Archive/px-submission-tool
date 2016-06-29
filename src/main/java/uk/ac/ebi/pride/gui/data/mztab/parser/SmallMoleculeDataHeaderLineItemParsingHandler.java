package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.SmallMoleculeData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 13:52
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class SmallMoleculeDataHeaderLineItemParsingHandler extends SmallMoleculeDataLineItemParsingHandler {
    private static final String MZTAB_SMALL_MOLECULE_DATA_HEADER_KEYWORD = "SMH";

    protected static class ColumnTypeMapper {
        public static SmallMoleculeData.ColumnType getColumnTypeFor(String token) {
            if (token.matches("^identifier$")) {
                return SmallMoleculeData.ColumnType.IDENTIFIER;
            }
            if (token.matches("^chemical_formula$")) {
                return SmallMoleculeData.ColumnType.CHEMICAL_FORMULA;
            }
            if (token.matches("^smiles$")) {
                return SmallMoleculeData.ColumnType.SMILES;
            }
            if (token.matches("^inchi_key$")) {
                return SmallMoleculeData.ColumnType.INCHI_KEY;
            }
            if (token.matches("^description$")) {
                return SmallMoleculeData.ColumnType.DESCRIPTION;
            }
            if (token.matches("^exp_mass_to_charge$")) {
                return SmallMoleculeData.ColumnType.EXP_MASS_TO_CHARGE;
            }
            if (token.matches("^calc_mass_to_charge$")) {
                return SmallMoleculeData.ColumnType.CALC_MASS_TO_CHARGE;
            }
            if (token.matches("^charge$")) {
                return SmallMoleculeData.ColumnType.CHARGE;
            }
            // TODO There is another error in the documentation here
            if (token.matches("^retention_time$")) {
                return SmallMoleculeData.ColumnType.RETENTION_TIME;
            }
            if (token.matches("^taxid$")) {
                return SmallMoleculeData.ColumnType.TAXID;
            }
            if (token.matches("^species$")) {
                return SmallMoleculeData.ColumnType.SPECIES;
            }
            if (token.matches("^database$")) {
                return SmallMoleculeData.ColumnType.DATABASE;
            }
            if (token.matches("^database_version$")) {
                return SmallMoleculeData.ColumnType.DATABASE_VERSION;
            }
            if (token.matches("^spectra_ref$")) {
                return SmallMoleculeData.ColumnType.SPECTRA_REF;
            }
            if (token.matches("^search_engine$")) {
                return SmallMoleculeData.ColumnType.SEARCH_ENGINE;
            }
            if (token.matches("^best_search_engine_score\\[\\d+\\]$")) {
                return SmallMoleculeData.ColumnType.BEST_SEARCH_ENGINE_SCORE;
            }
            if (token.matches("^modifications$")) {
                return SmallMoleculeData.ColumnType.MODIFICATIONS;
            }
            if (token.matches("^smallmolecule_abundance_assay\\[\\d+\\]$")) {
                return SmallMoleculeData.ColumnType.SMALLMOLECULE_ABUNDANCE_ASSAY;
            }
            if (token.matches("^smallmolecule_abundance_study_variable\\[\\d+\\]$")) {
                return SmallMoleculeData.ColumnType.SMALLMOLECULE_ABUNDANCE_STUDY_VARIABLE;
            }
            if (token.matches("^smallmolecule_stdev_study_variable\\[\\d+\\]$")) {
                return SmallMoleculeData.ColumnType.SMALLMOLECULE_STDEV_STUDY_VARIABLE;
            }
            if (token.matches("^smallmolecule_std_error_study_variable\\[\\d+\\]$")) {
                return SmallMoleculeData.ColumnType.SMALLMOLECULE_STD_ERROR_STUDY_VARIABLE;
            }
            if (token.matches("^search_engine_score\\[\\d+\\]_ms_run\\[\\d+\\]$")) {
                return SmallMoleculeData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN;
            }
            if (token.startsWith("opt_")) {
                return SmallMoleculeData.ColumnType.OPT_CUSTOM_ATTRIBUTE;
            }
            if (token.matches("^reliability$")) {
                return SmallMoleculeData.ColumnType.RELIABILITY;
            }
            if (token.matches("^uri$")) {
                return SmallMoleculeData.ColumnType.URI;
            }
            // Unrecognized column type
            return null;
        }
    }

    private void checkForDuplicatedHeader(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getSmallMoleculeDataSection().hasHeaderBeenSpecified()) {
            throw new LineItemParsingHandlerException("DUPLICATED Small Molecule HEADER found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if (lineItems[0].equals(MZTAB_SMALL_MOLECULE_DATA_HEADER_KEYWORD)) {
            checkForDuplicatedHeader(context, lineNumber);
            try {
                return doProcessHeaderColumns(context, lineItems, lineNumber, offset);
            } catch (Exception e) {
                throw new LineItemParsingHandlerException(e.getMessage());
            }
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
