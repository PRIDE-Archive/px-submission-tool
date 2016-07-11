package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.PeptideSearchEngineScore;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-06 11:00
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabPeptideSearchEngineScoreLineItemParsingHandler extends MzTabPeptideSearchEngineScoreLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getPeptideSearchEngineScoreFromContext(context, getIndex()) != null) {
            throw new LineItemParsingHandlerException("DUPLICATED " + MZTAB_PEPTIDE_SEARCH_ENGINE_SCORE_KEY + " with index '" + getIndex() + "'");
        }
    }

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        setPeptideSearchEngineScoreInContext(context, getIndex(), new PeptideSearchEngineScore(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
