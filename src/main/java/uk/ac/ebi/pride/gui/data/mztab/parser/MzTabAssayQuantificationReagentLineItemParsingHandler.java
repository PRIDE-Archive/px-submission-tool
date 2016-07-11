package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:54
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabAssayQuantificationReagentLineItemParsingHandler extends MzTabAssayLineItemParsingHandler {
    protected static final String MZTAB_ASSAY_QUANTIFICATION_REAGENT_PROPERTY_KEY = "quantification_reagent";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        if (getPropertyKey().equals(MZTAB_ASSAY_QUANTIFICATION_REAGENT_PROPERTY_KEY)) {
            return doProcessEntry(context, lineNumber, offset);
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset);
}
