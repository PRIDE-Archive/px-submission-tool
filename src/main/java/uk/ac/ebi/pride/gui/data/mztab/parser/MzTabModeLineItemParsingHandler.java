package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.List;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-14 16:14
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * At this level of the hierarchy we capture the information for a particular item, the strategy for processing that
 * information will be defined by subclasses
 */

public abstract class MzTabModeLineItemParsingHandler extends MetaDataLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MzTabModeLineItemParsingHandler.class);

    protected static final String MZTAB_MODE_KEYWORD = "mzTab-mode";

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if ((lineItems.length == 3)
                && (lineItems[1].equals(MZTAB_MODE_KEYWORD))) {
            if (lineItems[2].equals(MetaData.MzTabMode.COMPLETE.getValue())) {
                return doProcessMode(context, line, lineNumber, offset, MetaData.MzTabMode.COMPLETE);
            }
            if (lineItems[2].equals(MetaData.MzTabMode.SUMMARY.getValue())) {
                return doProcessMode(context, line, lineNumber, offset, MetaData.MzTabMode.SUMMARY);
            }
            throw new LineItemParsingHandlerException("mzTab mode '" + lineItems[2] + "' NOT RECOGNIZED");
        }
        return false;
    }

    // Delegate strategy
    protected abstract boolean doProcessMode(MzTabParser context, String line, long lineNumber, long offset, MetaData.MzTabMode mode) throws LineItemParsingHandlerException;
}
