package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.Arrays;
import java.util.Collection;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-29 10:55
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class SectionHeaderProcessingStrategiesParsingStackUnrecognizedColumnTokensTest {

    private MzTabParser context;
    // Test case bean
    private String testDescription;
    private String headerLine;
    private LineItemParsingHandler testSubject;

    public SectionHeaderProcessingStrategiesParsingStackUnrecognizedColumnTokensTest(String testDescription, String headerLine, LineItemParsingHandler testSubject) {
        this.testDescription = testDescription;
        this.headerLine = headerLine;
        this.testSubject = testSubject;
    }

    @Before
    public void prepareEmptyContext() {
        context = new DummyMzTabParser("dummy.file");
    }

    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectUnrecognizedColumnTypes() {
        testSubject.parseLine(context, headerLine, 1, 0);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testsToRun() {
        // Test configurations
        return Arrays.asList(new Object[][] {
                // test description, expected column type (null if expected), column token, test subject
                {"Unrecognized column type is rejected", "PRH\tñlakjsdfklj", new QuickProteinDataHeaderLineItemParsingHandler()},
                {"Unrecognized column type is rejected", "PEH\tñlakjsdfklj", new QuickPeptideDataHeaderLineItemParsingHandler()},
                {"Unrecognized column type is rejected", "PSH\tñlakjsdfklj", new QuickPsmDataHeaderLineItemParsingHandler()},
                {"Unrecognized column type is rejected", "SMH\tñlakjsdfklj", new QuickSmallMoleculeDataHeaderLineItemParsingHandler()}
        });
    }
}