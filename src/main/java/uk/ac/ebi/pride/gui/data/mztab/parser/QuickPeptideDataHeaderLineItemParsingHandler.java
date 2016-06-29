package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.PeptideData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 14:12
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickPeptideDataHeaderLineItemParsingHandler extends PeptideDataHeaderLineItemParsingHandler {
    @Override
    protected boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException {
        for (int i = 0; i < parsedHeaderTokens.length; i++) {
            String headerToken = parsedHeaderTokens[i];
            PeptideData.ColumnType columnType = PeptideDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor(headerToken);
            if (columnType == null) {
                throw new LineItemParsingHandlerException("UNKNOWN Peptide Header Column '" + headerToken + "'");
            }
            context.getPeptideDataSection().addColumn(i, columnType);
        }
        return true;
    }
}
