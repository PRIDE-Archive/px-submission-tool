package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.PeptideData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 13:08
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class PeptideDataHeaderLineItemParsingHandler extends PeptideDataLineItemParsingHandler {
    private static final String MZTAB_PEPTIDE_DATA_HEADER_KEYWORD = "PEH";

    protected static class ColumnTypeMapper {
        public static PeptideData.ColumnType getColumnTypeFor(String token) {
            if (token.matches("^sequence")) {
                return PeptideData.ColumnType.SEQUENCE;
            }
            if (token.matches("^accession$")) {
                return PeptideData.ColumnType.ACCESSION;
            }
            if (token.matches("^unique")) {
                return PeptideData.ColumnType.UNIQUE;
            }
            if (token.matches("^database$")) {
                return PeptideData.ColumnType.DATABASE;
            }
            if (token.matches("^database_version$")) {
                return PeptideData.ColumnType.DATABASE_VERSION;
            }
            if (token.matches("^search_engine$")) {
                return PeptideData.ColumnType.SEARCH_ENGINE;
            }
            if (token.matches("^best_search_engine_score\\[\\d+\\]$")) {
                return PeptideData.ColumnType.BEST_SEARCH_ENGINE_SCORE;
            }
            if (token.matches("^modifications$")) {
                return PeptideData.ColumnType.MODIFICATIONS;
            }
            if (token.matches("^retention_time$")) {
                return PeptideData.ColumnType.RETENTION_TIME;
            }
            if (token.matches("^retention_time_window$")) {
                return PeptideData.ColumnType.RETENTION_TIME_WINDOW;
            }
            if (token.matches("^charge$")) {
                return PeptideData.ColumnType.CHARGE;
            }
            if (token.matches("^mass_to_charge$")) {
                return PeptideData.ColumnType.MASS_TO_CHARGE;
            }
            if (token.matches("^peptide_abundance_study_variable\\[\\d+\\]$")) {
                return PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STUDY_VARIABLE;
            }
            if (token.matches("^peptide_abundance_stdev_study_variable\\[\\d+\\]$")) {
                return PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STDEV_STUDY_VARIABLE;
            }
            if (token.matches("^peptide_abundance_std_error_study_variable\\[\\d+\\]$")) {
                return PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STD_ERROR_STUDY_VARIABLE;
            }
            if (token.matches("^search_engine_score\\[\\d+\\]_ms_run\\[\\d+\\]$")) {
                return PeptideData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN;
            }
            if (token.matches("^peptide_abundance_assay\\[\\d+\\]$")) {
                return PeptideData.ColumnType.PEPTIDE_ABUNDANCE_ASSAY;
            }
            if (token.matches("^spectra_ref$")) {
                return PeptideData.ColumnType.SPECTRA_REF;
            }
            if (token.startsWith("opt_")) {
                return PeptideData.ColumnType.OPT_CUSTOM_ATTIBUTE;
            }
            if (token.matches("^reliability$")) {
                return PeptideData.ColumnType.RELIABILITY;
            }
            if (token.matches("^uri$")) {
                return PeptideData.ColumnType.URI;
            }
            // Unrecognized column type
            return null;
        }
    }

    private void checkForDuplicatedHeader(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getPeptideDataSection().hasHeaderBeenSpecified()) {
            throw new LineItemParsingHandlerException("DUPLICATED Peptide HEADER found at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if (lineItems[0].equals(MZTAB_PEPTIDE_DATA_HEADER_KEYWORD)) {
            checkForDuplicatedHeader(context, lineNumber);
            try {
                return doProcessHeaderColumns(context, lineItems, lineNumber, offset);
            } catch (Exception e) {
                throw new LineItemParsingHandlerException(e.getMessage());
            }
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
