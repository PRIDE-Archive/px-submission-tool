package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.PeptideQuantificationUnit;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 11:58
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabPeptideQuantificationUnitLineItemParsingHandler extends MzTabPeptideQuantificationUnitLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getMetaDataSection().getPeptideQuantificationUnit() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED peptide quantification method entry found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        context.getMetaDataSection().setPeptideQuantificationUnit(new PeptideQuantificationUnit(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
