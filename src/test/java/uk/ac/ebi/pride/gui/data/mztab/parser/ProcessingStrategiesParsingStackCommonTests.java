package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-23 11:54
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
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
        this.context = context;
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
        return Arrays.asList(new Object[][]{
                // Bulk testing of parsing/not parsing conditions
                {"", new QuickMzTabMsRunFormatLineItemParsingHandler(), "ms-run format quick strategy returns false for empty line", false},
                {"", new QuickMzTabMsRunIdFormatLineItemParsingHandler(), "ms-run id format quick strategy returns false for empty line", false},
                {"", new QuickMzTabMsRunLocationLineItemParsingHandler(), "ms-run location quick processing strategy returns false for empty line", false},
                {"", new QuickMzTabSampleSpeciesLineItemParsingHandler(), "sample species quick processing strategy returns false for empty line", false},
                {"", new QuickMzTabSampleTissueLineItemParsingHandler(), "sample tissue quick processing strategy returns false for empty line", false},
                {"", new QuickMzTabSampleCellTypeLineItemParsingHandler(), "sample cell type quick processing strategy returns false for empty line", false},
                {"", new QuickMzTabSampleCustomLineItemParsingHandler(), "sample custom attribute quick processing strategy returns false for empty line", false},
                {"", new QuickMzTabSampleDiseaseLineItemParsingHandler(), "sample disease attribute quick processing strategy returns false for empty line", false}
        });
    }
}