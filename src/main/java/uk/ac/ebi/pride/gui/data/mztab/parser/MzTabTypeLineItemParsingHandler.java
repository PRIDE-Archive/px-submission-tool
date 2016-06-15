package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-15 16:05
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MzTabTypeLineItemParsingHandler extends MetaDataLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MzTabTypeLineItemParsingHandler.class);

    // Keyword
    protected static final String MZTAB_TYPE_KEYWORD = "mzTab-type";

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if ((lineItems.length == 3) && (lineItems[1].equals(MZTAB_TYPE_KEYWORD))) {
            if (lineItems[2].equals(MetaData.MzTabType.IDENTIFICATION.getValue())) {
                return doProcessType(context, line, lineNumber, offset, MetaData.MzTabType.IDENTIFICATION);
            }
            if (lineItems[2].equals(MetaData.MzTabType.QUANTIFICATION.getValue())) {
                return doProcessType(context, line, lineNumber, offset, MetaData.MzTabType.QUANTIFICATION);
            }
            throw new LineItemParsingHandlerException("mzTab type '" + lineItems[2] + "' NOT RECOGNIZED");
        }
        return false;
    }

    // Delegate processing strategy
    protected abstract boolean doProcessType(MzTabParser context, String line, long lineNumber, long offset, MetaData.MzTabType type) throws LineItemParsingHandlerException ;
}
