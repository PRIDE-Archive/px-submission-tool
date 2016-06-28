package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.ProteinData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-28 14:00
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class ProteinDataHeaderLineItemParsingHandler extends ProteinDataLineItemParsingHandler {
    private static final String MZTAB_PROTEIN_DATA_HEADER_KEYWORD = "PRH";

    protected static class ColumnTypeMapper {
        public static ProteinData.ColumnType getColumnTypeFor(String token) {
            if (token.matches("^accession$")) {
                return ProteinData.ColumnType.ACCESSION;
            }
            if (token.matches("^description$")) {
                return ProteinData.ColumnType.DESCRIPTION;
            }
            if (token.matches("^taxid$")) {
                return ProteinData.ColumnType.TAXID;
            }
            if (token.matches("^species$")) {
                return ProteinData.ColumnType.SPECIES;
            }
            if (token.matches("^database$")) {
                return ProteinData.ColumnType.DATABASE;
            }
            if (token.matches("^database_version$")) {
                return ProteinData.ColumnType.DATABASE_VERSION;
            }
            if (token.matches("^search_engine$")) {
                return ProteinData.ColumnType.SEARCH_ENGINE;
            }
            if (token.matches("^best_search_engine_score\\[\\d+\\]$")) {
                return ProteinData.ColumnType.BEST_SEARCH_ENGINE_SCORE;
            }
            if (token.matches("^ambiguity_members$")) {
                return ProteinData.ColumnType.AMBIGUITY_MEMBERS;
            }
            if (token.matches("^modifications$")) {
                return ProteinData.ColumnType.MODIFICATIONS;
            }
            if (token.matches("^protein_coverage$")) {
                return ProteinData.ColumnType.PROTEIN_COVERAGE;
            }
            if (token.matches("^protein_abundance_study_variable\\[\\d+\\]$")) {
                return ProteinData.ColumnType.PROTEIN_ABUNDANCE_STUDY_VARIABLE;
            }
            if (token.matches("^protein_abundance_stdev_study_variable\\[\\d+\\]$")) {
                return ProteinData.ColumnType.PROTEIN_ABUNDANCE_STDEV_STUDY_VARIABLE;
            }
            if (token.matches("^protein_abundance_std_error_study_variable\\[\\d+\\]$")) {
                return ProteinData.ColumnType.PROTEIN_ABUNDANCE_STD_ERROR_STUDY_VARIABLE;
            }
            if (token.matches("^search_engine_score\\[\\d+\\]_ms_run\\[\\d+\\]$")) {
                return ProteinData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN;
            }
            if (token.matches("^num_psms_ms_run\\[\\d+\\]$")) {
                return ProteinData.ColumnType.NUM_PSMS_MS_RUN;
            }
            if (token.matches("^num_peptides_distinct_ms_run\\[\\d+\\]$")) {
                return ProteinData.ColumnType.NUM_PEPTIDES_DISTINCT_MS_RUN;
            }
            // WARNING - Correct this in the documentation
            if (token.matches("^num_peptides_unique_ms_run\\[\\d+\\]$")) {
                return ProteinData.ColumnType.NUM_PEPTIDES_UNIQUE_MS_RUN;
            }
            if (token.matches("^protein_abundance_assay\\[\\d+\\]$")) {
                return ProteinData.ColumnType.PROTEIN_ABUNDANCE_ASSAY;
            }
            if (token.startsWith("opt_")) {
                return ProteinData.ColumnType.OPT_CUSTOM_ATTIBUTE;
            }
            if (token.matches("^go_terms$")) {
                return ProteinData.ColumnType.GO_TERMS;
            }
            if (token.matches("^reliability$")) {
                return ProteinData.ColumnType.RELIABILITY;
            }
            if (token.matches("^uri$")) {
                return ProteinData.ColumnType.URI;
            }
            return null;
        }
    }

    // Check for duplicated section entry
    private void checkForDuplicated(MzTabParser context, long lineNumber) {
        if (context.getProteinDataSection().hasHeaderBeenSpecified()) {
            throw new LineItemParsingHandlerException("DUPLICATED Protein HEADER found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if (lineItems[0].equals(MZTAB_PROTEIN_DATA_HEADER_KEYWORD)) {
            // Protein section header
            checkForDuplicated(context, lineNumber);
            return doProcessHeaderColumns(context, lineItems, lineNumber, offset);
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessHeaderColumns(MzTabParser context, String[] lineItems, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
