package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.HashSet;
import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:12
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabStudyVariableAssayRefLineItemParsingHandler extends MzTabStudyVariableLineItemParsingHandler {
    protected static final String MZTAB_STUDY_VARIABLE_ASSAY_REF_PROPERTY_KEY = "assay_refs";
    protected static final String ASSAY_REF_VALUE_KEY = "assay";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        try {
            if (getPropertyKey().equals(MZTAB_STUDY_VARIABLE_ASSAY_REF_PROPERTY_KEY)) {
                Set<Integer> parsedAssayRefIndexes = new HashSet<>();
                for (String assayRef :
                        getPropertyValue().split(",")) {
                    parsedAssayRefIndexes.add(ParsingHelper.getIndexInSquareBracketsFromIndexedKeyword(ASSAY_REF_VALUE_KEY, assayRef));
                }
                return doProcessEntry(context, lineNumber, offset, parsedAssayRefIndexes);
            }
        } catch (Exception e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    // Delegate processing strategy
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, Set<Integer> parsedAssayRefIndexes) throws LineItemParsingHandlerException;
}
