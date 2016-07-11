package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.QuantificationReagent;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:56
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabAssayQuantificationReagentLineItemParsingHandler extends MzTabAssayQuantificationReagentLineItemParsingHandler {
    public void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getAssayFromContext(context, getIndex()).getQuantificationReagent() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED quantification reagent information for assay with index '" + getIndex() + "' at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) {
        checkForDuplicatedEntry(context, lineNumber);
        getAssayFromContext(context, getIndex()).setQuantificationReagent(new QuantificationReagent(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
