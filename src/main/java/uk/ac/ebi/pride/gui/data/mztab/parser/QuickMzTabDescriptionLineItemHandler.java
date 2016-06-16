package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-16 14:02
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Quick processing strategy for mzTab description items
 */

public class QuickMzTabDescriptionLineItemHandler extends MzTabDescriptionLineItemHandler {
    private void checkForDuplicatedDescription(MzTabParser context, long lineNumber) {
        if (context.getMetaDataSection().getDescription() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED " + MZTAB_DESCRIPTION_KEYWORD + " entry found at line " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessDescription(MzTabParser context, String line, long lineNumber, long offset, String description) throws LineItemParsingHandlerException {
        checkForDuplicatedDescription(context, lineNumber);
        context.getMetaDataSection().setDescription(description);
        return true;
    }
}
