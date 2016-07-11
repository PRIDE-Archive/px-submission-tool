package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.Assay;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:50
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabAssayMsRunRefLineItemParsingHandler extends MzTabAssayMsRunRefLineItemParsingHandler {
    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, int parsedMsRunRefIndex) throws LineItemParsingHandlerException {
        Assay assay = getAssayFromContext(context, getIndex());
        if (assay.hasMsRunRefBeenSet()) {
            throw new LineItemParsingHandlerException("DUPLICATED ms_run_ref entry for assay with index '" + getIndex() + "' at line '" + lineNumber + "'");
        }
        assay.setMsRunRefIndex(parsedMsRunRefIndex);
        return true;
    }
}
