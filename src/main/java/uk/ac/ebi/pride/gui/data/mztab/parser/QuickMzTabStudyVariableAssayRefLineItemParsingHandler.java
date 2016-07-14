package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.StudyVariable;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 15:15
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabStudyVariableAssayRefLineItemParsingHandler extends MzTabStudyVariableAssayRefLineItemParsingHandler {
    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset, Set<Integer> parsedAssayRefIndexes) throws LineItemParsingHandlerException {
        StudyVariable studyVariable = getStudyVariableFromContext(context, getIndex());
        // Check if sample refs have already been specified for this study variable
        if (!studyVariable.getReportedAssayRefIndexes().isEmpty()) {
            throw new LineItemParsingHandlerException("DUPLICATED assay_refs specification for study_variable with index '" + getIndex() + "' at line '" + lineNumber + "'");
        }
        for (int index :
                parsedAssayRefIndexes) {
            studyVariable.addAssayRefIndex(index);
        }
        return true;
    }
}
