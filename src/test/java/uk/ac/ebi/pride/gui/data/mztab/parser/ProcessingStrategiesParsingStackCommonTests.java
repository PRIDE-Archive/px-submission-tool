package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-23 11:54
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * This is a bulk test to make sure that certain group of strategies have the same behavior in the parsing stack
 */

@RunWith(Parameterized.class)
public class ProcessingStrategiesParsingStackCommonTests {
    private String rawParsingLine;
    private LineItemParsingHandler testSubject;
    private DummyMzTabParser context;
    private String testDescription;
    private boolean expectedParseLineReturnValue;

    public ProcessingStrategiesParsingStackCommonTests(String rawParsingLine,
                                                       LineItemParsingHandler testSubject,
                                                       //DummyMzTabParser context,
                                                       String testDescription,
                                                       boolean expectedParseLineReturnValue) {
        this.rawParsingLine = rawParsingLine;
        this.testSubject = testSubject;
        this.testDescription = testDescription;
        this.expectedParseLineReturnValue = expectedParseLineReturnValue;
    }

    @Before
    public void prepareEmptyContext() {
        context = new DummyMzTabParser("dummyfile");
    }

    @Test
    public void notParsableLine() {
        assertThat(testDescription, testSubject.parseLine(context, rawParsingLine, 1, 0), is(expectedParseLineReturnValue));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testsToRun() {
        // Not intended for mapping purposes
        Map<String, String> malformedStrings = new HashMap<>(); // (description, malformed_string)
        malformedStrings.put("line with more elements than expected", "MTD\tlkjsdfk\tñlkjsdf\tñlkjsdfglkjh");
        malformedStrings.put("line with less elements than expected", "MTD\tlkjsdfk");

        // Test subjects
        Map<String, LineItemParsingHandler> testSubjects = new HashMap<>();
        testSubjects.put("ms-run format quick processing strategy", new QuickMzTabMsRunFormatLineItemParsingHandler());
        testSubjects.put("ms-run id format quick processing strategy", new QuickMzTabMsRunIdFormatLineItemParsingHandler());
        testSubjects.put("ms-run location quick processing strategy", new QuickMzTabMsRunLocationLineItemParsingHandler());
        testSubjects.put("sample species quick processing strategy", new QuickMzTabSampleSpeciesLineItemParsingHandler());
        testSubjects.put("sample tissue quick processing strategy", new QuickMzTabSampleTissueLineItemParsingHandler());
        testSubjects.put("sample cell type quick processing strategy", new QuickMzTabSampleCellTypeLineItemParsingHandler());
        testSubjects.put("sample custom attribute quick processing strategy", new QuickMzTabSampleCustomLineItemParsingHandler());
        testSubjects.put("sample disease quick processing strategy", new QuickMzTabSampleDiseaseLineItemParsingHandler());
        testSubjects.put("mzTab description quick processing strategy", new QuickMzTabDescriptionLineItemHandler());
        testSubjects.put("mzTab file ID quick processing strategy", new QuickMzTabDescriptionLineItemHandler());
        testSubjects.put("mzTab mode quick processing strategy", new QuickMzTabModeLineItemParsingHandler());
        testSubjects.put("mzTab title quick processing strategy", new QuickMzTabTitleLineItemParsingHandler());
        testSubjects.put("mzTab type quick processing strategy", new QuickMzTabTypeLineItemParsingHandler());
        testSubjects.put("mzTab version quick processing strategy", new QuickMzTabVersionLineItemParsingHandler());


        // Bulk tests
        List<Object[]> bulkTests = new ArrayList<>();
        // Empty line tests
        for (String item :
                testSubjects.keySet()) {
            bulkTests.add(new Object[]{"", testSubjects.get(item), item + " test for empty line", false});
        }
        // Malformed lines
        for (String testDescription :
                malformedStrings.keySet()) {
            for (String subjectDescriptionItem : testSubjects.keySet()) {
                bulkTests.add(new Object[]{malformedStrings.get(testDescription), testSubjects.get(subjectDescriptionItem), subjectDescriptionItem + " test for " + testDescription, false});
            }
        }
        return bulkTests;
    }
}