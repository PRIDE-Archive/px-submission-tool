package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.SmallMoleculeData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 14:18
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickSmallMoleculeDataHeaderLineItemParsingHandler extends SmallMoleculeDataHeaderLineItemParsingHandler {
    @Override
    protected boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException {
        for (int i = 0; i < parsedHeaderTokens.length; i++) {
            String headerToken = parsedHeaderTokens[i];
            SmallMoleculeData.ColumnType columnType = SmallMoleculeDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor(headerToken);
            if (columnType == null) {
                throw new LineItemParsingHandlerException("UNKNOWN Small Molecule Header Column '" + headerToken + "'");
            }
            context.getSmallMoleculeDataSection().addColumn(i, columnType);
        }
        return true;
    }
}
