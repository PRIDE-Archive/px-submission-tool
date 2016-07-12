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
 * Timestamp: 2016-07-11 16:00
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class MetadataIndexedLineItemWithPropertyProcessingStrategiesParsingStackTest {
    private String lineStart;
    private String lineItemKey;
    private int index;
    private String propertyKey;
    private String propertyValue;
    private String testDescription;
    private LineItemParsingHandler subject;
    private DummyMzTabParser context;

    public MetadataIndexedLineItemWithPropertyProcessingStrategiesParsingStackTest(String lineStart, String lineItemKey, int index, String propertyKey, String propertyValue, String testDescription, LineItemParsingHandler subject) {
        this.lineStart = lineStart;
        this.lineItemKey = lineItemKey;
        this.index = index;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
        this.testDescription = testDescription;
        this.subject = subject;
    }

    @Before
    public void prepareEmptyContext() {
        this.context = new DummyMzTabParser("dummyfile");
    }

    // Test for manipulated line item property key
    // TODO - Refactor-out this test to evaluate this aspect of those line items with properties
    @Test
    public void rejectIncorrectPropertyKey() {
        assertThat("mzTab line rejected because of incorrect property key, by " + testDescription,
                subject.parseLine(context, TestsLittleHelper.getMzTabLineForIndexedLineItemWithProperty(lineStart, lineItemKey, index, "lkasjdf", propertyValue), 1, 0),
                is(false));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> subjectsToTest() {
        return Arrays.asList(new Object[][]{
                {"MTD", "ms_run", 110, "format", "[PSI-MS, MS:1001062, Mascot MGF file, ]", "ms_run format", new QuickMzTabMsRunFormatLineItemParsingHandler()},
                {"MTD", "ms_run", 111, "id_format", "[PSI-MS, MS:1000774, multiple peak list nativeID format, ]", "ms_run id_format", new QuickMzTabMsRunIdFormatLineItemParsingHandler()},
                {"MTD", "ms_run", 112, "location", "file:Z:\\Proteomics\\CGehring\\ITurek\\20131123-1nM-Rep3\\Scaffold\\CM\\All3repSequest_Mascot mzIdent export 15-Sep-2014 04-39-39-AM\\TMT Sample 1\\Mudpit_20131006-IL-a24 (F027066).mzid_Mudpit_20131006-IL-a24_(F027066).MGF", "ms_run location", new QuickMzTabMsRunLocationLineItemParsingHandler()},
                {"MTD", "ms_run", 112, "hash", "de9f2c7fd25e1b3afad3e85a0bd17d9b100db4b3", "ms_run hash", new QuickMzTabMsRunHashLineItemParsingHandler()},
                {"MTD", "ms_run", 112, "hash_method", "[MS, MS: MS:1000569, SHA-1, ]", "ms_run hash_method", new QuickMzTabMsRunHashMethodLineItemParsingHandler()},
                {"MTD", "fixed_mod", 121, "site", "N-term", "fixed modification site", new QuickMzTabFixedModSiteLineItemParsingHandler()},
                {"MTD", "fixed_mod", 122, "position", "Protein N-term", "fixed modification position", new QuickMzTabFixedModPositionLineItemParsingHandler()},
                {"MTD", "variable_mod", 123, "site", "N-term", "variable modification site", new QuickMzTabVariableModSiteLineItemParsingHandler()},
                {"MTD", "variable_mod", 124, "position", "Protein N-term", "variable modification position", new QuickMzTabVariableModPositionLineItemParsingHandler()},
                {"MTD", "study_variable", 125, "description", "Group B (spike-in 0.74 fmol/uL)", "study_variable description", new QuickMzTabStudyVariableDescriptionLineItemParsingHandler()},
                {"MTD", "study_variable", 126, "sample_refs", "sample[1]", "study_variable sample refs", new QuickMzTabStudyVariableSampleRefLineItemParsingHandler()},
                {"MTD", "study_variable", 127, "assay_refs", "assay[1], assay[2], assay[3]", "study_variable assay refs", new QuickMzTabStudyVariableAssayRefLineItemParsingHandler()},
                {"MTD", "assay", 128, "sample_ref", "sample[1]", "assay sample ref", new QuickMzTabAssaySampleRefLineItemParsingHandler()},
                {"MTD", "assay", 129, "ms_run_ref", "ms_run[1]", "assay ms_run ref", new QuickMzTabAssayMsRunRefLineItemParsingHandler()},
                {"MTD", "assay", 130, "quantification_reagent", "[PRIDE,PRIDE:0000114,iTRAQ reagent,114]", "assay quantification_reagent", new QuickMzTabAssayQuantificationReagentLineItemParsingHandler()},
                {"MTD", "software", 132, "setting", "Fragment tolerance = 0.1 Da", "software setting", new QuickMzTabSoftwareSettingLineItemParsingHandler()}
        });
    }

}
