package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-15 16:32
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class QuickMzTabTypeLineItemParsingHandlerTest {
    private static MzTabParser context;
    private LineItemParsingHandler subject;

    @BeforeClass
    public static void initDummies() {
        context = new DummyMzTabParser("dummyfile.txt");
    }

    @Before
    public void init() {
        subject = new QuickMzTabTypeLineItemParsingHandler();
    }

    @Test
    public void returnFalseOnEmptyLine() {
        assertFalse("Return false when handling an empty line", subject.parseLine(context, "", 1, 0));
    }

    @Test
    public void returnFalseWhenInvalidKeyword() {
        assertFalse("Return false when invalid keyword", subject.parseLine(context, "MTD\tikjshdfksjhfd\tGarbage information", 1, 0));
    }

    @Test
    public void processIdentificationType() {
        MzTabParser context = Mockito.mock(DummyMzTabParser.class);
        MetaData metaData = Mockito.mock(MetaData.class);
        String mztabCompleteModeLine = "MTD\tmzTab-type\t" + MetaData.MzTabType.IDENTIFICATION.getValue();
        when(context.getMetaDataSection()).thenReturn(metaData);
        subject.parseLine(context, mztabCompleteModeLine, 1, 0);
        verify(context, atLeastOnce()).getMetaDataSection();
        verify(metaData, times(1)).getType();
        verify(metaData, times(1)).setType(MetaData.MzTabType.IDENTIFICATION);
    }

    @Test
    public void processQuantificationType() {
        MzTabParser context = Mockito.mock(DummyMzTabParser.class);
        MetaData metaData = Mockito.mock(MetaData.class);
        String mztabCompleteModeLine = "MTD\tmzTab-type\t" + MetaData.MzTabType.QUANTIFICATION.getValue();
        when(context.getMetaDataSection()).thenReturn(metaData);
        subject.parseLine(context, mztabCompleteModeLine, 1, 0);
        verify(context, atLeastOnce()).getMetaDataSection();
        verify(metaData, times(1)).getType();
        verify(metaData, times(1)).setType(MetaData.MzTabType.QUANTIFICATION);
    }
}