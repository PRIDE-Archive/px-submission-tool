package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.StudyVariable;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 14:55
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabStudyVariableSampleRefLineItemParsingHandler extends MzTabStudyVariableSampleRefLineItemParsingHandler {
    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, Set<Integer> parsedSampleRefs) throws LineItemParsingHandlerException {
        StudyVariable studyVariable = getStudyVariableFromContext(context, getIndex());
        // Check if sample refs have already been specified for this study variable
        if (!studyVariable.getReportedSampleRefIndexes().isEmpty()) {
            throw new LineItemParsingHandlerException("DUPLICATED sample_refs specification for study_variable with index '" + getIndex() + "' at line '" + lineNumber + "'");
        }
        for (int index :
                parsedSampleRefs) {
            studyVariable.addSampleRefIndex(index);
        }
        return true;
    }
}
