package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.ebi.pride.gui.data.mztab.model.PsmData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 16:10
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class PsmColumnTypeMapperTest {
    private String testDescription;
    private String parsedToken;
    private PsmData.ColumnType expectedColumnType;

    // Proxy tester
    private static class TestSubject extends PsmDataHeaderLineItemParsingHandler {
        public static PsmData.ColumnType getColumnTypeFor(String token) {
            return ColumnTypeMapper.getColumnTypeFor(token);
        }

        @Override
        protected boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException {
            return false;
        }
    }

    public PsmColumnTypeMapperTest(String testDescription, String parsedToken, PsmData.ColumnType expectedColumnType) {
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
        Map<String, PsmData.ColumnType> testingTokens = new HashMap<>();
        testingTokens.put("sequence", PsmData.ColumnType.SEQUENCE);
        testingTokens.put("PSM_ID", PsmData.ColumnType.PSM_ID);
        testingTokens.put("accession", PsmData.ColumnType.ACCESSION);
        testingTokens.put("unique", PsmData.ColumnType.UNIQUE);
        testingTokens.put("database", PsmData.ColumnType.DATABASE);
        testingTokens.put("database_version", PsmData.ColumnType.DATABASE_VERSION);
        testingTokens.put("search_engine", PsmData.ColumnType.SEARCH_ENGINE);
        testingTokens.put("search_engine_score[1]", PsmData.ColumnType.SEARCH_ENGINE_SCORE);
        testingTokens.put("modifications", PsmData.ColumnType.MODIFICATIONS);
        testingTokens.put("spectra_ref", PsmData.ColumnType.SPECTRA_REF);
        testingTokens.put("retention_time", PsmData.ColumnType.RETENTION_TIME);
        testingTokens.put("charge", PsmData.ColumnType.CHARGE);
        testingTokens.put("exp_mass_to_charge", PsmData.ColumnType.EXP_MASS_TO_CHARGE);
        testingTokens.put("calc_mass_to_charge", PsmData.ColumnType.CALC_MASS_TO_CHARGE);
        testingTokens.put("pre", PsmData.ColumnType.PRE);
        testingTokens.put("post", PsmData.ColumnType.POST);
        testingTokens.put("start", PsmData.ColumnType.START);
        testingTokens.put("end", PsmData.ColumnType.END);
        testingTokens.put("opt_my_custom_attribute", PsmData.ColumnType.OPT_CUSTOM_ATTRIBUTE);
        testingTokens.put("reliability", PsmData.ColumnType.RELIABILITY);
        testingTokens.put("uri", PsmData.ColumnType.URI);
        for (String token :
                testingTokens.keySet()) {
            testCases.add(new Object[] {token + " column type", token, testingTokens.get(token)});
        }
        return testCases;
    }
}
