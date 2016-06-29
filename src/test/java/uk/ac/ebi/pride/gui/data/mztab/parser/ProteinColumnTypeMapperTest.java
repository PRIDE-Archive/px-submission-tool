package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.pride.gui.data.mztab.model.ProteinData;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-28 15:16
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class ProteinColumnTypeMapperTest {

    // Let's explore the testing of a protected inner class via a class adapter and proxy pattern
    // NOTE - I don't like the way I implemented this
    private class TestSubject extends ProteinDataHeaderLineItemParsingHandler {

        // It may look un-necessary to test all columns just for matching, but it will save time later in the future in
        // case we want to do anything with the matching data later, e.g. capturing indexes
        public void testAccessionColumnTypeMapping() {
            assertThat("accession column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("accession").equals(ProteinData.ColumnType.ACCESSION), is(true));
        }

        public void testDescriptionColumnTypeMapping() {
            assertThat("description column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("description").equals(ProteinData.ColumnType.DESCRIPTION), is(true));
        }

        public void testTaxidColumnTypeMapping() {
            assertThat("taxid column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("taxid").equals(ProteinData.ColumnType.TAXID), is(true));
        }

        public void testSpeciesColumnTypeMapping() {
            assertThat("species column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("species").equals(ProteinData.ColumnType.SPECIES), is(true));
        }

        public void testDatabaseColumnTypeMapping() {
            assertThat("database column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("database").equals(ProteinData.ColumnType.DATABASE), is(true));
        }

        public void testDatabaseVersionColumnTypeMapping() {
            assertThat("database_version column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("database_version").equals(ProteinData.ColumnType.DATABASE_VERSION), is(true));
        }

        public void testSearchEngineColumnTypeMapping() {
            assertThat("search_engine column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("search_engine").equals(ProteinData.ColumnType.SEARCH_ENGINE), is(true));
        }

        public void testBestSearchEngineScoreColumnTypeMapping() {
            assertThat("best_search_engine_score column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("best_search_engine_score[1]").equals(ProteinData.ColumnType.BEST_SEARCH_ENGINE_SCORE), is(true));
        }

        public void testAmbiguityMembersColumnType() {
            assertThat("ambiguity_members column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("ambiguity_members").equals(ProteinData.ColumnType.AMBIGUITY_MEMBERS));
        }

        public void testModificationsColumnTypeMapping() {
            assertThat("modifications column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("modifications").equals(ProteinData.ColumnType.MODIFICATIONS));
        }

        public void testProteinCoverageColumnTypeMapping() {
            assertThat("protein_coverage column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("protein_coverage").equals(ProteinData.ColumnType.PROTEIN_COVERAGE));
        }

        public void testProteinAbundanceStudyVariableColumnTypeMapping() {
            assertThat("protein_abundance_study_variable column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("protein_abundance_study_variable[1]").equals(ProteinData.ColumnType.PROTEIN_ABUNDANCE_STUDY_VARIABLE));
        }

        public void testProteinAbundanceStdevStudyVariableColumnTypeMapping() {
            assertThat("protein_abundance_stdev_study_variable column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("protein_abundance_stdev_study_variable[1]").equals(ProteinData.ColumnType.PROTEIN_ABUNDANCE_STDEV_STUDY_VARIABLE));
        }

        public void testProteinAbundanceStdErrorStudyVariableColumnTypeMapping() {
            assertThat("protein_abundance_std_error_study_variable column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("protein_abundance_std_error_study_variable[1]").equals(ProteinData.ColumnType.PROTEIN_ABUNDANCE_STD_ERROR_STUDY_VARIABLE));
        }

        public void testSearchEngineScoreMsRunColumnTypeMapping() {
            assertThat("search_engine_score[1-n]_ms_run[1-n] column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("search_engine_score[1]_ms_run[1]").equals(ProteinData.ColumnType.SEARCH_ENGINE_SCORE_MS_RUN));
        }

        public void testNumPsmsMsRunColumnTypeMapping() {
            assertThat("num_psms_ms_run[1-n] column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("num_psms_ms_run[1]").equals(ProteinData.ColumnType.NUM_PSMS_MS_RUN));
        }

        public void testNumPeptidesDistinctMsRunColumnTypeMapping() {
            assertThat("num_peptides_distinct_ms_run[1-n] column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("num_peptides_distinct_ms_run[1]").equals(ProteinData.ColumnType.NUM_PEPTIDES_DISTINCT_MS_RUN));
        }

        public void testNumPeptidesUniqueMsRunColumnTypeMapping() {
            assertThat("num_peptides_unique_ms_run[1-n] column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("num_peptides_unique_ms_run[1]").equals(ProteinData.ColumnType.NUM_PEPTIDES_UNIQUE_MS_RUN));
        }

        public void testProteinAbundanceAssayColumnTypeMapping() {
            assertThat("protein_abundance_assay[1-n] column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("protein_abundance_assay[1]").equals(ProteinData.ColumnType.PROTEIN_ABUNDANCE_ASSAY));
        }

        public void testCustomAttributeColumnTypeMapping() {
            assertThat("custom attribute column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("opt_global_protein_sequence").equals(ProteinData.ColumnType.OPT_CUSTOM_ATTIBUTE));
        }

        public void testGoTermsColumnTypeMapping() {
            assertThat("go_terms column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("go_terms").equals(ProteinData.ColumnType.GO_TERMS));
        }

        public void testReliabilityColumnTypeMapping() {
            assertThat("reliability column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("reliability").equals(ProteinData.ColumnType.RELIABILITY));
        }

        public void testUriColumnTypeMapping() {
            assertThat("uri column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("uri").equals(ProteinData.ColumnType.URI));
        }

        @Override
        protected boolean doProcessHeaderColumns(MzTabParser context, String[] lineItems, long lineNumber, long offset) throws LineItemParsingHandlerException {
            // Dummy override to test protected inner class
            return false;
        }
    }

    private TestSubject testSubject;

    @Before
    public void before() {
        testSubject = new TestSubject();
    }

    @Test
    public void accessionColumnType() {
        testSubject.testAccessionColumnTypeMapping();
    }

    @Test
    public void testDescriptionColumnTypeMapping() {
        testSubject.testDescriptionColumnTypeMapping();
    }

    @Test
    public void testTaxidColumnTypeMapping() {
        testSubject.testTaxidColumnTypeMapping();
    }

    @Test
    public void testSpeciesColumnTypeMapping() {
        testSubject.testSpeciesColumnTypeMapping();
    }

    @Test
    public void testDatabaseColumnTypeMapping() {
        testSubject.testDatabaseColumnTypeMapping();
    }

    @Test
    public void testDatabaseVersionColumnTypeMapping() {
        testSubject.testDatabaseVersionColumnTypeMapping();
    }

    @Test
    public void testSearchEngineColumnTypeMapping() {
        testSubject.testSearchEngineColumnTypeMapping();
    }

    @Test
    public void testBestSearchEngineScoreColumnTypeMapping() {
        testSubject.testBestSearchEngineScoreColumnTypeMapping();
    }

    @Test
    public void testAmbiguityMembersColumnType() {
        testSubject.testAmbiguityMembersColumnType();
    }

    @Test
    public void testModificationsColumnTypeMapping() {
        testSubject.testModificationsColumnTypeMapping();
    }

    @Test
    public void testProteinCoverageColumnTypeMapping() {
        testSubject.testProteinCoverageColumnTypeMapping();
    }

    @Test
    public void testProteinAbundanceStudyVariableColumnTypeMapping() {
        testSubject.testProteinAbundanceStudyVariableColumnTypeMapping();
    }

    @Test
    public void testProteinAbundanceStdevStudyVariableColumnTypeMapping() {
        testSubject.testProteinAbundanceStdevStudyVariableColumnTypeMapping();
    }

    @Test
    public void testProteinAbundanceStdErrorStudyVariableColumnTypeMapping() {
        testSubject.testProteinAbundanceStdErrorStudyVariableColumnTypeMapping();
    }

    @Test
    public void testSearchEngineScoreMsRunColumnTypeMapping() {
        testSubject.testSearchEngineScoreMsRunColumnTypeMapping();
    }

    @Test
    public void testNumPsmsMsRunColumnTypeMapping() {
        testSubject.testNumPsmsMsRunColumnTypeMapping();
    }

    @Test
    public void testNumPeptidesDistinctMsRunColumnTypeMapping() {
        testSubject.testNumPeptidesDistinctMsRunColumnTypeMapping();
    }

    @Test
    public void testNumPeptidesUniqueMsRunColumnTypeMapping() {
        testSubject.testNumPeptidesUniqueMsRunColumnTypeMapping();
    }

    @Test
    public void testProteinAbundanceAssayColumnTypeMapping() {
        testSubject.testProteinAbundanceAssayColumnTypeMapping();
    }

    @Test
    public void testCustomAttributeColumnTypeMapping() {
        testSubject.testCustomAttributeColumnTypeMapping();
    }

    @Test
    public void testGoTermsColumnTypeMapping() {
        testSubject.testGoTermsColumnTypeMapping();
    }

    @Test
    public void testReliabilityColumnTypeMapping() {
        testSubject.testReliabilityColumnTypeMapping();
    }

    @Test
    public void testUriColumnTypeMapping() {
        testSubject.testUriColumnTypeMapping();
    }
}