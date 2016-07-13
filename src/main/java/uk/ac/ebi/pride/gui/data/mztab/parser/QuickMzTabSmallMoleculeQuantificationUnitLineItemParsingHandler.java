package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.SmallMoleculeQuantificationUnit;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 11:55
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabSmallMoleculeQuantificationUnitLineItemParsingHandler extends MzTabSmallMoleculeQuantificationUnitLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getMetaDataSection().getSmallMoleculeQuantificationUnit() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED small molecule quantification method entry found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        context.getMetaDataSection().setSmallMoleculeQuantificationUnit(new SmallMoleculeQuantificationUnit(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
