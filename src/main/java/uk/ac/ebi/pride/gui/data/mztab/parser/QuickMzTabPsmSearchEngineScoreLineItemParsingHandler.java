package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.PsmSearchEngineScore;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-06 11:09
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabPsmSearchEngineScoreLineItemParsingHandler extends MzTabPsmSearchEngineScoreLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getPsmSearchEngineScoreForContext(context, getIndex()) != null) {
            throw new LineItemParsingHandlerException("DUPLICATED " + MZTAB_PSM_SEARCH_ENGINE_SCORE_KEY + " with index '" + getIndex() + "'");
        }
    }

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        setPsmSearchEngineScoreInContext(context, getIndex(), new PsmSearchEngineScore(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
