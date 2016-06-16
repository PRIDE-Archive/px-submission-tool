package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-16 14:23
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MzTabFileIdLineItemParsingHandler extends MetaDataLineItemParsingHandler {
    // Keyword
    protected static final String MZTAB_FILE_ID_KEYWORD = "mzTab-ID";

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if ((lineItems.length == 3) && (lineItems[1].equals(MZTAB_FILE_ID_KEYWORD))) {
            return doProcessFileId(context, line, lineNumber, offset, lineItems[2]);
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessFileId(MzTabParser context, String line, long lineNumber, long offset, String fileId) throws LineItemParsingHandlerException;
}
