package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-15 16:15
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class QuickMzTabTypeLineItemParsingHandler extends MzTabTypeLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(QuickMzTabTypeLineItemParsingHandler.class);

    private void checkForTypeDuplication(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        if (context.getMetaDataSection().getType() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED '" + MZTAB_TYPE_KEYWORD + "' entry found!, line number '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doProcessType(MzTabParser context, String line, long lineNumber, long offset, MetaData.MzTabType type) throws LineItemParsingHandlerException {
        checkForTypeDuplication(context, line, lineNumber, offset);
        context.getMetaDataSection().setType(type);
        return true;
    }
}
