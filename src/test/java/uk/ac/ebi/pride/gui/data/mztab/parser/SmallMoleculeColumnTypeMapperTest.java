package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.ebi.pride.gui.data.mztab.model.SmallMoleculeData;
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
public class SmallMoleculeColumnTypeMapperTest {
    private String testDescription;
    private String parsedToken;
    private SmallMoleculeData.ColumnType expectedColumnType;

    // Proxy tester
    private static class TestSubject extends SmallMoleculeDataHeaderLineItemParsingHandler {
        public static SmallMoleculeData.ColumnType getColumnTypeFor(String token) {
            return ColumnTypeMapper.getColumnTypeFor(token);
        }

        @Override
        protected boolean doProcessHeaderColumns(MzTabParser context, String[] parsedHeaderTokens, long lineNumber, long offset) throws LineItemParsingHandlerException {
            return false;
        }
    }

    public SmallMoleculeColumnTypeMapperTest(String testDescription, String parsedToken, SmallMoleculeData.ColumnType expectedColumnType) {
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
        Map<String, SmallMoleculeData.ColumnType> testingTokens = new HashMap<>();
        testingTokens.put("identifier", SmallMoleculeData.ColumnType.IDENTIFIER);
        testingTokens.put("chemical_formula", SmallMoleculeData.ColumnType.CHEMICAL_FORMULA);
        testingTokens.put("smiles", SmallMoleculeData.ColumnType.SMILES);
        testingTokens.put("inchi_key", SmallMoleculeData.ColumnType.INCHI_KEY);
        testingTokens.put("description", SmallMoleculeData.ColumnType.DESCRIPTION);
        testingTokens.put("exp_mass_to_charge", SmallMoleculeData.ColumnType.EXP_MASS_TO_CHARGE);
        testingTokens.put("calc_mass_to_charge", SmallMoleculeData.ColumnType.CALC_MASS_TO_CHARGE);
        testingTokens.put("charge", SmallMoleculeData.ColumnType.CHARGE);
        testingTokens.put("retention_time", SmallMoleculeData.ColumnType.RETENTION_TIME);
        testingTokens.put("taxid", SmallMoleculeData.ColumnType.TAXID);
        testingTokens.put("species", SmallMoleculeData.ColumnType.SPECIES);
        testingTokens.put("database", SmallMoleculeData.ColumnType.DATABASE);
        testingTokens.put("database_version", SmallMoleculeData.ColumnType.DATABASE_VERSION);
        testingTokens.put("spectra_ref", SmallMoleculeData.ColumnType.SPECTRA_REF);
        testingTokens.put("search_engine", SmallMoleculeData.ColumnType.SEARCH_ENGINE);
        testingTokens.put("best_search_engine_score[1]", SmallMoleculeData.ColumnType.BEST_SEARCH_ENGINE_SCORE);
        testingTokens.put("modifications", SmallMoleculeData.ColumnType.MODIFICATIONS);
        testingTokens.put("smallmolecule_abundance_assay[1]", SmallMoleculeData.ColumnType.SMALLMOLECULE_ABUNDANCE_ASSAY);
        testingTokens.put("smallmolecule_abundance_study_variable[1]", SmallMoleculeData.ColumnType.SMALLMOLECULE_ABUNDANCE_STUDY_VARIABLE);
        testingTokens.put("smallmolecule_stdev_study_variable[1]", SmallMoleculeData.ColumnType.SMALLMOLECULE_STDEV_STUDY_VARIABLE);
        testingTokens.put("smallmolecule_std_error_study_variable[1]", SmallMoleculeData.ColumnType.SMALLMOLECULE_STD_ERROR_STUDY_VARIABLE);
        testingTokens.put("search_engine_score[1]_ms_run[1]", SmallMoleculeData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN);
        testingTokens.put("opt_my_custom_attribute", SmallMoleculeData.ColumnType.OPT_CUSTOM_ATTRIBUTE);
        testingTokens.put("reliability", SmallMoleculeData.ColumnType.RELIABILITY);
        testingTokens.put("uri", SmallMoleculeData.ColumnType.URI);
        for (String token :
                testingTokens.keySet()) {
            testCases.add(new Object[] {token + " column type", token, testingTokens.get(token)});
        }
        return testCases;
    }
}
