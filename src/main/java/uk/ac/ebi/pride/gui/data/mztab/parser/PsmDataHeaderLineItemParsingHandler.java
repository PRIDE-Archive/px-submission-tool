package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.PsmData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.Arrays;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 13:40
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class PsmDataHeaderLineItemParsingHandler extends PsmDataLineItemParsingHandler {
    private static final String MZTAB_PSM_DATA_HEADER_KEYWORD = "PSH";

    protected static class ColumnTypeMapper {
        public static PsmData.ColumnType getColumnTypeFor(String token) {
            if (token.matches("^sequence$")) {
                return PsmData.ColumnType.SEQUENCE;
            }
            if (token.matches("^PSM_ID$")) {
                return PsmData.ColumnType.PSM_ID;
            }
            if (token.matches("^accession$")) {
                return PsmData.ColumnType.ACCESSION;
            }
            if (token.matches("^unique$")) {
                return PsmData.ColumnType.UNIQUE;
            }
            if (token.matches("^database$")) {
                return PsmData.ColumnType.DATABASE;
            }
            if (token.matches("^database_version$")) {
                return PsmData.ColumnType.DATABASE_VERSION;
            }
            if (token.matches("^search_engine$")) {
                return PsmData.ColumnType.SEARCH_ENGINE;
            }
            if (token.matches("^search_engine_score\\[\\d+\\]$")) {
                return PsmData.ColumnType.SEARCH_ENGINE_SCORE;
            }
            if (token.matches("^modifications$")) {
                return PsmData.ColumnType.MODIFICATIONS;
            }
            if (token.matches("^spectra_ref$")) {
                return PsmData.ColumnType.SPECTRA_REF;
            }
            if (token.matches("^retention_time$")) {
                return PsmData.ColumnType.RETENTION_TIME;
            }
            if (token.matches("^charge$")) {
                return PsmData.ColumnType.CHARGE;
            }
            if (token.matches("^exp_mass_to_charge$")) {
                return PsmData.ColumnType.EXP_MASS_TO_CHARGE;
            }
            if (token.matches("^calc_mass_to_charge$")) {
                return PsmData.ColumnType.CALC_MASS_TO_CHARGE;
            }
            if (token.matches("^pre$")) {
                return PsmData.ColumnType.PRE;
            }
            if (token.matches("^post$")) {
                return PsmData.ColumnType.POST;
            }
            if (token.matches("^start$")) {
                return PsmData.ColumnType.START;
            }
            if (token.matches("^end$")) {
                return PsmData.ColumnType.END;
            }
            if (token.startsWith("opt_")) {
                return PsmData.ColumnType.OPT_CUSTOM_ATTRIBUTE;
            }
            if (token.matches("^reliability$")) {
                return PsmData.ColumnType.RELIABILITY;
            }
            if (token.matches("^uri$")) {
                return PsmData.ColumnType.URI;
            }
            // Unrecognized column type
            return null;
        }
    }

    public void checkForDucplicatedHeader(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getPsmDataSection().hasHeaderBeenSpecified()) {
            throw new LineItemParsingHandlerException("DUPLICATED PSM HEADER found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if (lineItems[0].equals(MZTAB_PSM_DATA_HEADER_KEYWORD)) {
            checkForDucplicatedHeader(context, lineNumber);
            try {
                return doProcessHeaderColumns(context, Arrays.copyOfRange(lineItems, 1, lineItems.length), lineNumber, offset);
            } catch (Exception e) {
                throw new LineItemParsingHandlerException(e.getMessage());
            }
        }
        return false;
    }

    // Delegate Processing
    protected abstract boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
