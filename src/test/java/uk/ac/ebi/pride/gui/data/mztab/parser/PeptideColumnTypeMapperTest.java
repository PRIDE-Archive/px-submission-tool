package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.ebi.pride.gui.data.mztab.model.PeptideData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 16:09
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class PeptideColumnTypeMapperTest {
    private String testDescription;
    private String parsedToken;
    private PeptideData.ColumnType expectedColumnType;

    private static class TestSubject extends PeptideDataHeaderLineItemParsingHandler {
        public static PeptideData.ColumnType getColumnTypeFor(String token) {
            return PeptideDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor(token);
        }
        @Override
        protected boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException {
            return false;
        }
    }

    public PeptideColumnTypeMapperTest(String testDescription, String parsedToken, PeptideData.ColumnType expectedColumnType) {
        this.testDescription = testDescription;
        this.parsedToken = parsedToken;
        this.expectedColumnType = expectedColumnType;
    }

    @Test
    public void testBody() {
        assertThat(testDescription, TestSubject.getColumnTypeFor(parsedToken).equals(expectedColumnType), is(true));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getTestCases() {
        List<Object[]> testCases = new ArrayList<>();
        Map<String, PeptideData.ColumnType> testingTokens = new HashMap<>();
        testingTokens.put("sequence", PeptideData.ColumnType.SEQUENCE);
        testingTokens.put("accession", PeptideData.ColumnType.ACCESSION);
        testingTokens.put("unique", PeptideData.ColumnType.UNIQUE);
        testingTokens.put("database", PeptideData.ColumnType.DATABASE);
        testingTokens.put("database_version", PeptideData.ColumnType.DATABASE_VERSION);
        testingTokens.put("search_engine", PeptideData.ColumnType.SEARCH_ENGINE);
        testingTokens.put("best_search_engine_score[1]", PeptideData.ColumnType.BEST_SEARCH_ENGINE_SCORE);
        testingTokens.put("modifications", PeptideData.ColumnType.MODIFICATIONS);
        testingTokens.put("retention_time", PeptideData.ColumnType.RETENTION_TIME);
        testingTokens.put("retention_time_window", PeptideData.ColumnType.RETENTION_TIME_WINDOW);
        testingTokens.put("charge", PeptideData.ColumnType.CHARGE);
        testingTokens.put("mass_to_charge", PeptideData.ColumnType.MASS_TO_CHARGE);
        testingTokens.put("peptide_abundance_study_variable[1]", PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STUDY_VARIABLE);
        testingTokens.put("peptide_abundance_stdev_study_variable[1]", PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STDEV_STUDY_VARIABLE);
        testingTokens.put("peptide_abundance_std_error_study_variable[1]", PeptideData.ColumnType.PEPTIDE_ABUNDANCE_STD_ERROR_STUDY_VARIABLE);
        testingTokens.put("search_engine_score[1]_ms_run[1]", PeptideData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN);
        testingTokens.put("peptide_abundance_assay[1]", PeptideData.ColumnType.PEPTIDE_ABUNDANCE_ASSAY);
        testingTokens.put("spectra_ref", PeptideData.ColumnType.SPECTRA_REF);
        testingTokens.put("opt_my_custom_column_type", PeptideData.ColumnType.OPT_CUSTOM_ATTIBUTE);
        testingTokens.put("reliability", PeptideData.ColumnType.RELIABILITY);
        testingTokens.put("uri", PeptideData.ColumnType.URI);
        for (String token :
                testingTokens.keySet()) {
            testCases.add(new Object[] {token + " column type", token, testingTokens.get(token)});
        }
        return testCases;
    }
}
