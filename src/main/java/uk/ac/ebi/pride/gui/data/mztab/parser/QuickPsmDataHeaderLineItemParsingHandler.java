package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.PsmData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 14:15
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickPsmDataHeaderLineItemParsingHandler extends PsmDataHeaderLineItemParsingHandler {
    @Override
    protected boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException {
        for (int i = 0; i < parsedHeaderTokens.length; i++) {
            String headerToken = parsedHeaderTokens[i];
            PsmData.ColumnType columnType = PsmDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor(headerToken);
            if (columnType == null) {
                throw new LineItemParsingHandlerException("UNKNOWN PSM Header Column '" + headerToken + "'");
            }
            context.getPsmDataSection().addColumn(i, columnType);
        }
        return true;
    }
}
