package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-16 12:46
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Quick processing strategy for mzTab title
 */
public class QuickMzTabTitleLineItemParsingHandler extends MzTabTitleLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(QuickMzTabTitleLineItemParsingHandler.class);

    private void checkForDuplicatedTitle(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getMetaDataSection().getTitle() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED " + MZTAB_TITLE_KEYWORD + " found at line " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessTitle(MzTabParser context, String line, long lineNumber, long offset, String title) throws LineItemParsingHandlerException {
        checkForDuplicatedTitle(context, lineNumber);
        context.getMetaDataSection().setTitle(title);
        return true;
    }
}
