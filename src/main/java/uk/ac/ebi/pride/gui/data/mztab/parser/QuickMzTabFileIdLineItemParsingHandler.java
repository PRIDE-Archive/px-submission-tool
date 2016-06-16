package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-16 14:34
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class QuickMzTabFileIdLineItemParsingHandler extends MzTabFileIdLineItemParsingHandler {
    private void checkForDuplicatedFileId(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getMetaDataSection().getFileId() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED " + MZTAB_FILE_ID_KEYWORD + " entry found at line " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessFileId(MzTabParser context, String line, long lineNumber, long offset, String fileId) throws LineItemParsingHandlerException {
        checkForDuplicatedFileId(context, lineNumber);
        context.getMetaDataSection().setFileId(fileId);
        return true;
    }
}
