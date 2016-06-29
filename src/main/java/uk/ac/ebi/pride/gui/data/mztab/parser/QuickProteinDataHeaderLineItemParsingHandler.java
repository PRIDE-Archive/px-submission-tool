package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.ProteinData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 10:29
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickProteinDataHeaderLineItemParsingHandler extends ProteinDataHeaderLineItemParsingHandler {
    @Override
    protected boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException {
        for (int i = 0; i < parsedHeaderTokens.length; i++) {
            String headerToken = parsedHeaderTokens[i];
            ProteinData.ColumnType columnType = ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor(headerToken);
            if (columnType == null) {
                throw new LineItemParsingHandlerException("UNKNOWN Protein Header Column '" + headerToken + "'");
            }
            context.getProteinDataSection().addColumn(i, columnType);
        }
        return true;
    }
}
