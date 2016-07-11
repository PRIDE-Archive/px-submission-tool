package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.Assay;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:48
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabAssaySampleRefLineItemParsingHandler extends MzTabAssaySampleRefLineItemParsingHandler {
    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, int parsedSampleRefIndex) throws LineItemParsingHandlerException {
        Assay assay = getAssayFromContext(context, getIndex());
        if (assay.hasSampleRefBeenSet()) {
            throw new LineItemParsingHandlerException("DUPLICATED sample ref entry for assay with index '" + getIndex() + "' at line '" + lineNumber + "'");
        }
        assay.setSampleRefIndex(parsedSampleRefIndex);
        return true;
    }
}
