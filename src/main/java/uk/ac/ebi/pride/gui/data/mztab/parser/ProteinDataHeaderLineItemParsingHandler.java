package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.ProteinData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-28 14:00
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class ProteinDataHeaderLineItemParsingHandler extends ProteinDataLineItemParsingHandler {
    private static final String MZTAB_PROTEIN_DATA_HEADER_KEYWORD = "PRH";

    protected static class ColumnTypeMapper {
        public static ProteinData.ColumnType getColumnTypeFor(String token) {
            if (token.matches("^accession$")) {
                return ProteinData.ColumnType.ACCESSION;
            }
            return null;
        }
    }

    // Check for duplicated section entry
    private void checkForDuplicated(MzTabParser context, long lineNumber) {
        if (context.getProteinDataSection().hasHeaderBeenSpecified()) {
            throw new LineItemParsingHandlerException("DUPLICATED Protein HEADER found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if (lineItems[0].equals(MZTAB_PROTEIN_DATA_HEADER_KEYWORD)) {
            // Protein section header
            checkForDuplicated(context, lineNumber);
            return doProcessHeaderColumns(context, lineItems, lineNumber, offset);
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessHeaderColumns(MzTabParser context, String[] lineItems, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
