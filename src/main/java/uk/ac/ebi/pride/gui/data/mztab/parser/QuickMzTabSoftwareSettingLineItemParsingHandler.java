package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 16:54
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabSoftwareSettingLineItemParsingHandler extends MzTabSoftwareSettingLineItemParsingHandler {

    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getSoftwareEntryFromContext(context, getIndex()).getSetting(getPropertyEntryIndex()) != null) {
            throw new LineItemParsingHandlerException("DUPLICATED setting entry with subindex '" + getPropertyEntryIndex() + "' for software with index '" + getIndex() + "' at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        getSoftwareEntryFromContext(context, getIndex()).updateSetting(getPropertyEntryIndex(), getPropertyValue());
        return true;
    }
}
