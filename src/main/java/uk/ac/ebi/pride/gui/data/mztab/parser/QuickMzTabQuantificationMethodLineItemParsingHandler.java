package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.QuantificationMethod;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-12 8:27
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabQuantificationMethodLineItemParsingHandler extends MzTabQuantificationMethodLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getMetaDataSection().getQuantificationMethod() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED quantification method entry found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        context.getMetaDataSection().setQuantificationMethod(new QuantificationMethod(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
