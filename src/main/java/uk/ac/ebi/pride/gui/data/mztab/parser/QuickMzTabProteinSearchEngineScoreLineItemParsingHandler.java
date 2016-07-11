package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.ProteinSearchEngineScore;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-05 16:41
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabProteinSearchEngineScoreLineItemParsingHandler extends MzTabProteinSearchEngineScoreLineItemParsingHandler {
    // yes, checking for duplication on this attribute is part of processing it
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getProteinSearchEngineScoreFromContext(context, getIndex()) != null) {
            throw new LineItemParsingHandlerException("DUPLICATED " + MZTAB_PROTEIN_SEARCH_ENGINE_SCORE_KEY + " with index '" + getIndex() + "'");
        }
    }

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        setProteinSearchEngineScoreInContext(context, getIndex(), new ProteinSearchEngineScore(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
