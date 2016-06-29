package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import uk.ac.ebi.pride.gui.data.mztab.model.ProteinData;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 11:45
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class SectionHeaderProcessingStrategiesParsingStackExpectedColumnTypeTest {
    private MzTabParser context;
    // Test case bean
    private String testDescription;
    private Object expectedColumnType;
    private String headerLine;
    private LineItemParsingHandler testSubject;

    public SectionHeaderProcessingStrategiesParsingStackExpectedColumnTypeTest(String testDescription, Object expectedColumnType, String headerLine, LineItemParsingHandler testSubject) {
        this.testDescription = testDescription;
        this.expectedColumnType = expectedColumnType;
        this.headerLine = headerLine;
        this.testSubject = testSubject;
    }

    private class ProteinDataColumnTypeTest extends ProteinData {
        @Override
        public void addColumn(int index, ColumnType columnType) {
            assertThat("Column types match", columnType.equals(expectedColumnType), is(true));
        }
    }

    @Before
    public void prepareEmptyContext() {
        context = Mockito.mock(MzTabParser.class);
        when(context.getProteinDataSection()).thenReturn(new ProteinDataColumnTypeTest());
    }

    @Test()
    public void mainTest() {
        testSubject.parseLine(context, headerLine, 1, 0);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testsToRun() {
        // Test configurations
        return Arrays.asList(new Object[][] {
                // test description, expected column type (null if expected), column token, test subject
                {"Unrecognized column type is not recognized", ProteinData.ColumnType.DATABASE, "PRH\tdatabase", new QuickProteinDataHeaderLineItemParsingHandler()}
        });
    }
}
