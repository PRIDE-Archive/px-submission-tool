package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 16:47
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabSoftwareSettingLineItemParsingHandler extends MzTabSoftwareLineItemParsingHandler {
    protected static final String MZTAB_SOFTWARE_SETTING_PROPERTY_KEY = "setting";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset)  throws LineItemParsingHandlerException {
        if (getPropertyKey().equals(MZTAB_SOFTWARE_SETTING_PROPERTY_KEY)) {
            return doProcessEntry(context, lineNumber, offset);
        }
        return false;
    }

    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset)  throws LineItemParsingHandlerException;
}
