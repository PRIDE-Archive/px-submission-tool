package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-14 16:03
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This branch of the hierarchy processes information at section level for all those items in the metadata section of an
 * mzTab document.
 */

public abstract class MetaDataLineItemParsingHandler extends LineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataLineItemParsingHandler.class);

    protected interface IndexedItemWithProperty {
        String getLineItemKey();
        void setLineItemKey(String lineItemKey);
        int getIndex();
        void setIndex(int index);
        String getPropertyKey();
        void setPropertyKey(String pk);
        String getPropertyValue();
        void setPropertyValue(String pv);
    }

    @Override
    protected boolean doParseLine(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        return doParseLineItem(context, line, lineNumber, offset);
    }

    // Delegate processing of a particular item to subclass
    protected abstract boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
