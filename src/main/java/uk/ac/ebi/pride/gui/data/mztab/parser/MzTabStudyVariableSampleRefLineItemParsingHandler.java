package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 14:16
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabStudyVariableSampleRefLineItemParsingHandler extends MzTabStudyVariableLineItemParsingHandler {
    protected static final String MZTAB_STUDY_VARIABLE_SAMPLE_REF_PROPERTY_KEY = "sample_refs";
    protected static final String SAMPLE_REF_VALUE_KEY = "sample";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        try {
            if (getPropertyKey().equals(MZTAB_STUDY_VARIABLE_SAMPLE_REF_PROPERTY_KEY)) {
                Set<Integer> parsedSampleRefIndexes = new HashSet<>();
                for (String sampleRef :
                        getPropertyValue().split(",")) {
                    parsedSampleRefIndexes.add(ParsingHelper.getIndexInSquareBracketsFromIndexedKeyword(SAMPLE_REF_VALUE_KEY, sampleRef));
                }
                return doProcessEntry(context, lineNumber, offset, parsedSampleRefIndexes);
            }
        } catch (Exception e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    // Delegate processing strategy
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, Set<Integer> parsedSampleRefs) throws LineItemParsingHandlerException;
}
