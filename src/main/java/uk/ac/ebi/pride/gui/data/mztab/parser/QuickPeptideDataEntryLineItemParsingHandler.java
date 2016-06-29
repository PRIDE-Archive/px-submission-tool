package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 15:05
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickPeptideDataEntryLineItemParsingHandler extends PeptideDataEntryLineItemParsingHandler {
    @Override
    protected boolean doProcessDataEntryLine(MzTabParser context, String[] parsedColumnEntries, long lineNumber, long offset) throws LineItemParsingHandlerException {
        return parsedColumnEntries.length == context.getPeptideDataSection().getNumberOfColumns();
    }
}
