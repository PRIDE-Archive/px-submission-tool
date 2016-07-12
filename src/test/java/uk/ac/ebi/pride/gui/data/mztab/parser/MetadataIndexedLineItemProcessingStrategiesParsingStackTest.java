package uk.ac.ebi.pride.gui.data.mztab.parser;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-23 15:19
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
@NotThreadSafe
public class MetadataIndexedLineItemProcessingStrategiesParsingStackTest {
    private String lineStart;
    private String lineItemKey;
    private int index;
    private String propertyKey;
    private String propertyValue;
    private String testDescription;
    private LineItemParsingHandler subject;
    private DummyMzTabParser context;

    public MetadataIndexedLineItemProcessingStrategiesParsingStackTest(String lineStart,
                                                                       String lineItemKey,
                                                                       int index,
                                                                       String propertyKey,
                                                                       String propertyValue,
                                                                       String testDescription,
                                                                       LineItemParsingHandler subject) {
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

    // Test for manipulated line item key
    @Test
    public void rejectIncorrectLineItemKey() {
        assertThat("mzTab line rejected because of line item key mismatch, by " + testDescription,
                subject.parseLine(context, TestsLittleHelper.getMzTabLineForIndexedLineItemWithProperty(lineStart, "ñlkajsdflkj", index, propertyKey, propertyValue), 1, 0),
                is(false));
    }

    // Test for missing index
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithMissingIndex() {
        String mzTabLine = TestsLittleHelper.getMzTabLineForIndexedLineItemWithProperty(lineStart, lineItemKey, index, propertyKey, propertyValue);
        mzTabLine = mzTabLine.replaceFirst("\\[\\d+\\]", "[]");
        subject.parseLine(context, mzTabLine, 1, 0);
    }

    // Test for bad index formatting
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithBadIndexFormatting() {
        String mzTabLine = TestsLittleHelper.getMzTabLineForIndexedLineItemWithProperty(lineStart, lineItemKey, index, propertyKey, propertyValue);
        mzTabLine = mzTabLine.replaceFirst("\\[\\d+\\]", "[34.98]");
        subject.parseLine(context, mzTabLine, 1, 0);
    }

    // Test for NAN index
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithNanIndex() {
        String mzTabLine = TestsLittleHelper.getMzTabLineForIndexedLineItemWithProperty(lineStart, lineItemKey, index, propertyKey, propertyValue);
        mzTabLine = mzTabLine.replaceFirst("\\[\\d+\\]", "[Im_not_a_number]");
        subject.parseLine(context, mzTabLine, 1, 0);
    }

    // Test for duplicated entry
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectDuplicatedEntry() {
        String mzTabLine = TestsLittleHelper.getMzTabLineForIndexedLineItemWithProperty(lineStart, lineItemKey, index, propertyKey, propertyValue);
        subject.parseLine(context, mzTabLine, 1, 0);
        // Inserting the same line for the second time, triggers the error
        subject.parseLine(context, mzTabLine, 1, 0);
    }

    // Everything ok test
    @Test
    public void successfulParsing() {
        String mzTabLine = TestsLittleHelper.getMzTabLineForIndexedLineItemWithProperty(lineStart, lineItemKey, index, propertyKey, propertyValue);
        assertThat("mzTab line parses successfuly for " + this.testDescription,
                subject.parseLine(context, mzTabLine, 1, 0), is(true));
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
                {"MTD", "fixed_mod", 122, "", "[UNIMOD, UNIMOD:4, Carbamidomethyl, ]", "fixed modification value", new QuickMzTabFixedModValueLineItemParsingHandler()},
                {"MTD", "variable_mod", 123, "site", "N-term", "variable modification site", new QuickMzTabVariableModSiteLineItemParsingHandler()},
                {"MTD", "variable_mod", 124, "position", "Protein N-term", "variable modification position", new QuickMzTabVariableModPositionLineItemParsingHandler()},
                {"MTD", "variable_mod", 124, "", "[UNIMOD, UNIMOD:21, Phospho, ]", "variable modification value", new QuickMzTabVariableModValueLineItemParsingHandler()},
                {"MTD", "study_variable", 125, "description", "Group B (spike-in 0.74 fmol/uL)", "study_variable description", new QuickMzTabStudyVariableDescriptionLineItemParsingHandler()},
                {"MTD", "study_variable", 126, "sample_refs", "sample[1]", "study_variable sample refs", new QuickMzTabStudyVariableSampleRefLineItemParsingHandler()},
                {"MTD", "study_variable", 127, "assay_refs", "assay[1], assay[2], assay[3]", "study_variable assay refs", new QuickMzTabStudyVariableAssayRefLineItemParsingHandler()},
                {"MTD", "assay", 128, "sample_ref", "sample[1]", "assay sample ref", new QuickMzTabAssaySampleRefLineItemParsingHandler()},
                {"MTD", "assay", 129, "ms_run_ref", "ms_run[1]", "assay ms_run ref", new QuickMzTabAssayMsRunRefLineItemParsingHandler()},
                {"MTD", "assay", 130, "quantification_reagent", "[PRIDE,PRIDE:0000114,iTRAQ reagent,114]", "assay quantification_reagent", new QuickMzTabAssayQuantificationReagentLineItemParsingHandler()},
                {"MTD", "software", 131, "", "[MS, MS:1001207, Mascot, 2.3]", "software value", new QuickMzTabSoftwareValueLineItemParsingHandler()},
                {"MTD", "software", 132, "setting", "Fragment tolerance = 0.1 Da", "software setting", new QuickMzTabSoftwareSettingLineItemParsingHandler()},
                {"MTD", "protein_search_engine_score", 133, "", "[MS, MS:1001171, Mascot:score,]", "protein search engine score", new QuickMzTabProteinSearchEngineScoreLineItemParsingHandler()},
                {"MTD", "peptide_search_engine_score", 134, "", "[MS, MS:1001171, Mascot:score,]", "peptide search engine score", new QuickMzTabPeptideSearchEngineScoreLineItemParsingHandler()},
                {"MTD", "psm_search_engine_score", 135, "", "[MS, MS:1001330, X!Tandem:expect,]", "PSM search engine score", new QuickMzTabPsmSearchEngineScoreLineItemParsingHandler()},
                {"MTD", "smallmolecule_search_engine_score", 136, "", "[, , LipidDataAnalyzer,]", "Small Molecule search engine score", new QuickMzTabSmallMoleculeSearchEngineScoreLineItemParsingHandler()}
        });
    }
}
