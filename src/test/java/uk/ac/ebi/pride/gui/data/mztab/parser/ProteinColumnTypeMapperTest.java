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
    private class TestSubject extends ProteinDataHeaderLineItemParsingHandler {
        public void testAccessionColumnMapping() {
            assertThat("accession column type mapping", ProteinDataHeaderLineItemParsingHandler.ColumnTypeMapper.getColumnTypeFor("accession").equals(ProteinData.ColumnType.ACCESSION), is(true));
        }

        @Override
        protected boolean doProcessHeaderColumns(MzTabParser context, String[] lineItems, long lineNumber, long offset) throws LineItemParsingHandlerException {
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
        testSubject.testAccessionColumnMapping();
    }
}