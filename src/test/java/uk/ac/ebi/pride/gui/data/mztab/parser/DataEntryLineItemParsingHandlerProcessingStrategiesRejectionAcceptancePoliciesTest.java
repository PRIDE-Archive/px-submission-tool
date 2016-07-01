package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import uk.ac.ebi.pride.gui.data.mztab.model.PeptideData;
import uk.ac.ebi.pride.gui.data.mztab.model.ProteinData;
import uk.ac.ebi.pride.gui.data.mztab.model.PsmData;
import uk.ac.ebi.pride.gui.data.mztab.model.SmallMoleculeData;

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
public class DataEntryLineItemParsingHandlerProcessingStrategiesRejectionAcceptancePoliciesTest {

    private MzTabParser context;
    private String testDescription;
    private String testLine;
    private int numberOfFakeColumns;
    private boolean expectedParsingValue;
    private LineItemParsingHandler testSubject;

    public DataEntryLineItemParsingHandlerProcessingStrategiesRejectionAcceptancePoliciesTest(String testDescription, String testLine, int numberOfFakeColumns, boolean expectedParsingValue, LineItemParsingHandler testSubject) {
        this.testDescription = testDescription;
        this.testLine = testLine;
        this.numberOfFakeColumns = numberOfFakeColumns;
        this.expectedParsingValue = expectedParsingValue;
        this.testSubject = testSubject;
    }

    @Before
    public void emptyContext() {
        // Mock objects
        context = Mockito.mock(DummyMzTabParser.class);
        ProteinData  proteinData =  Mockito.mock(ProteinData.class);
        PeptideData peptideData = Mockito.mock(PeptideData.class);
        PsmData psmData = Mockito.mock(PsmData.class);
        SmallMoleculeData smallMoleculeData = Mockito.mock(SmallMoleculeData.class);
        // Manipulate reported columns
        when(context.getProteinDataSection()).thenReturn(proteinData);
        when(proteinData.getNumberOfColumns()).thenReturn(numberOfFakeColumns);
        when(context.getPeptideDataSection()).thenReturn(peptideData);
        when(peptideData.getNumberOfColumns()).thenReturn(numberOfFakeColumns);
        when(context.getPsmDataSection()).thenReturn(psmData);
        when(psmData.getNumberOfColumns()).thenReturn(numberOfFakeColumns);
        when(context.getSmallMoleculeDataSection()).thenReturn(smallMoleculeData);
        when(smallMoleculeData.getNumberOfColumns()).thenReturn(numberOfFakeColumns);
    }

    @Test
    public void testBody() {
        assertThat(testDescription, testSubject.parseLine(context, testLine, 1, 0), is(expectedParsingValue));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testsToRun() {
        // Test configurations
        return Arrays.asList(new Object[][] {
                // test description, expected column type (null if expected), column token, test subject
                {"Reject empty data entry line", "", 20, false, new QuickProteinDataEntryLineItemParsingHandler()},
                {"Reject not-protein data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", 20, false, new QuickProteinDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "PRT\tgenCDS_ENST00000621737_19_804389-808386_1\tgenCDS_ENST00000621737_19_804389-808386_1\t9606\tHomo sapiens (Human)\textra column\tanother extra column", 20, false, new QuickProteinDataEntryLineItemParsingHandler()},
                {"Accept valid protein data entry", "PRT\tgenCDS_ENST00000621737_19_804389-808386_1\tgenCDS_ENST00000621737_19_804389-808386_1\t9606\tHomo sapiens (Human)", 4, true, new QuickProteinDataEntryLineItemParsingHandler()},
                {"Reject empty data entry line", "", 20, false, new QuickPeptideDataEntryLineItemParsingHandler()},
                {"Reject not-peptide data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", 20, false, new QuickPeptideDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "PEP\tsome sample data for the first column\tmore sample data for the second column\t", 20, false, new QuickPeptideDataEntryLineItemParsingHandler()},
                {"Accept valid peptide entry", "PEP\tsome sample data for the first column\tmore sample data for the second column", 2, true, new QuickPeptideDataEntryLineItemParsingHandler()},
                {"Reject empty data entry line", "", 20, false, new QuickPsmDataEntryLineItemParsingHandler()},
                {"Reject not-PSM data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", 20, false, new QuickPsmDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "PSM\tsome sample data for the first column\tmore sample data for the second column\t", 20, false, new QuickPsmDataEntryLineItemParsingHandler()},
                {"Accept valid PSM data entry line", "PSM\tKYSVWJGGSJJASJSTFQQMWJSK\t1262\tgenCDS_ENST00000331789_7_5527748-5529657_-1", 3, true, new QuickPsmDataEntryLineItemParsingHandler()},
                {"Reject empty data entry line", "", 20, false, new QuickSmallMoleculeDataEntryLineItemParsingHandler()},
                {"Reject not-SmallMolecule data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", 20, false, new QuickSmallMoleculeDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "SML\tsome sample data for the first column\tmore sample data for the second column\t", 20, false, new QuickSmallMoleculeDataEntryLineItemParsingHandler()},
                {"Accept valid small molecule data entry line", "SML\tsome sample data for the first column\tmore sample data for the second column", 2, true, new QuickSmallMoleculeDataEntryLineItemParsingHandler()}
        });
    }
}