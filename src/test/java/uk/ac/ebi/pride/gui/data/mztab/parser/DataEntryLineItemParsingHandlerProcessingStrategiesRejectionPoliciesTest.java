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
        PeptideData peptideData = Mockito.mock(PeptideData.class);
        PsmData psmData = Mockito.mock(PsmData.class);
        SmallMoleculeData smallMoleculeData = Mockito.mock(SmallMoleculeData.class);
        // Manipulate reported columns
        when(context.getProteinDataSection()).thenReturn(proteinData);
        when(proteinData.getNumberOfColumns()).thenReturn(20);
        when(context.getPeptideDataSection()).thenReturn(peptideData);
        when(peptideData.getNumberOfColumns()).thenReturn(20);
        when(context.getPsmDataSection()).thenReturn(psmData);
        when(psmData.getNumberOfColumns()).thenReturn(20);
        when(context.getSmallMoleculeDataSection()).thenReturn(smallMoleculeData);
        when(smallMoleculeData.getNumberOfColumns()).thenReturn(20);
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
                {"Reject data entry line with missing/extra column", "PRT\tgenCDS_ENST00000621737_19_804389-808386_1\tgenCDS_ENST00000621737_19_804389-808386_1\t9606\tHomo sapiens (Human)\t", new QuickProteinDataEntryLineItemParsingHandler()},
                {"Reject empty data entry line", "", new QuickPeptideDataEntryLineItemParsingHandler()},
                {"Reject not-peptide data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", new QuickPeptideDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "PEP\tsome sample data for the first column\tmore sample data for the second column\t", new QuickPeptideDataEntryLineItemParsingHandler()},
                {"Reject empty data entry line", "", new QuickPsmDataEntryLineItemParsingHandler()},
                {"Reject not-peptide data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", new QuickPsmDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "PSM\tsome sample data for the first column\tmore sample data for the second column\t", new QuickPsmDataEntryLineItemParsingHandler()},
                {"Reject empty data entry line", "", new QuickSmallMoleculeDataEntryLineItemParsingHandler()},
                {"Reject not-peptide data entry line", "MTD\tsample[1]-species[1]\t[NEWT, 9606, Homo sapiens (Human), ]", new QuickSmallMoleculeDataEntryLineItemParsingHandler()},
                {"Reject data entry line with missing/extra column", "SML\tsome sample data for the first column\tmore sample data for the second column\t", new QuickSmallMoleculeDataEntryLineItemParsingHandler()}
        });
    }
}