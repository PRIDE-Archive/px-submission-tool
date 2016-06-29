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
 * Timestamp: 2016-06-29 12:33
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class DataEntryLineItemParsingHandlerProcessingStrategiesRejectionPoliciesTest {

    private MzTabParser context;
    private String testDescription;
    private String testLine;
    private LineItemParsingHandler testSubject;

    public DataEntryLineItemParsingHandlerProcessingStrategiesRejectionPoliciesTest(String testDescription, String testLine, LineItemParsingHandler testSubject) {
        this.testDescription = testDescription;
        this.testLine = testLine;
        this.testSubject = testSubject;
    }

    @Before
    public void emptyContext() {
        // Mock objects
        context = Mockito.mock(DummyMzTabParser.class);
        ProteinData  proteinData =  Mockito.mock(ProteinData.class);
        // Manipulate reported columns
        when(context.getProteinDataSection()).thenReturn(proteinData);
        when(proteinData.getNumberOfColumns()).thenReturn(20);
    }

    @Test
    public void testBody() {
        assertThat(testDescription, testSubject.parseLine(context, testLine, 1, 0), is(false));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testsToRun() {
        // Test configurations
        return Arrays.asList(new Object[][] {
                // test description, expected column type (null if expected), column token, test subject
                {"Reject empty data entry line", "", new QuickProteinDataEntryLineItemParsingHandler()},
                {"Reject not-protein data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", new QuickProteinDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "PRT\tgenCDS_ENST00000621737_19_804389-808386_1\tgenCDS_ENST00000621737_19_804389-808386_1\t9606\tHomo sapiens (Human)", new QuickProteinDataEntryLineItemParsingHandler()}
        });
    }
}