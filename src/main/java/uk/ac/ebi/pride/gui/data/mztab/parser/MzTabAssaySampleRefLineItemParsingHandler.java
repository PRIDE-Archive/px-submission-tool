package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:32
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabAssaySampleRefLineItemParsingHandler extends MzTabAssayLineItemParsingHandler {
    // TODO - The documentation says 'sample_refs' on page 11, and 'sample_ref' later on
    protected static final String MZTAB_ASSAY_SAMPLE_REF_PROPERTY_KEY = "sample_ref";
    protected static final String SAMPLE_REF_VALUE_KEY = "sample";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        try {
            if (getPropertyKey().equals(MZTAB_ASSAY_SAMPLE_REF_PROPERTY_KEY)) {
                return doProcessEntry(context, lineNumber, offset, ParsingHelper.getIndexInSquareBracketsFromIndexedKeyword(SAMPLE_REF_VALUE_KEY, getPropertyValue()));
            }
        } catch (Exception e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, int parsedSampleRefIndex) throws LineItemParsingHandlerException;
}
