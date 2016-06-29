package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.Arrays;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 14:58
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class PsmDataEntryLineItemParsingHandler extends PsmDataLineItemParsingHandler {
    private static final String MZTAB_PSM_DATA_ENTRY_LINE_KEYWORD = "PSM";

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if (lineItems[0].equals(MZTAB_PSM_DATA_ENTRY_LINE_KEYWORD)) {
            try {
                return doProcessDataEntryLine(context, Arrays.copyOfRange(lineItems, 1, lineItems.length), lineNumber, offset);
            } catch (Exception e) {
                throw new LineItemParsingHandlerException(e.getMessage());
            }
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessDataEntryLine(MzTabParser context, String[] parsedColumnEntries, long lineNumber, long offset) throws LineItemParsingHandlerException;

}
