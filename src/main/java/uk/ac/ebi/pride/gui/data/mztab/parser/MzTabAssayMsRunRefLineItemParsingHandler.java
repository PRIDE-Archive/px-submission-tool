package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:39
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabAssayMsRunRefLineItemParsingHandler extends MzTabAssayLineItemParsingHandler {
    protected static final String MZTAB_ASSAY_MS_RUN_REF_PROPERTY_KEY = "ms_run_ref";
    protected static final String MS_RUN_REF_TOKEN_KEY = "ms_run";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        try {
            if (getPropertyKey().equals(MZTAB_ASSAY_MS_RUN_REF_PROPERTY_KEY)) {
                return doProcessEntry(context, lineNumber, offset, ParsingHelper.getIndexInSquareBracketsFromIndexedKeyword(MS_RUN_REF_TOKEN_KEY, getPropertyValue()));
            }
        } catch (Exception e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, int parsedMsRunRefIndex) throws LineItemParsingHandlerException;
}
