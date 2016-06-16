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
 * Timestamp: 2016-06-16 14:37
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class QuickMzTabFileIdLineItemParsingHandlerTest {
    private static MzTabParser context;
    private LineItemParsingHandler subject;

    @BeforeClass
    public static void initDummies() {
        context = new DummyMzTabParser("dummyfile.txt");
    }

    @Before
    public void init() {
        subject = new QuickMzTabFileIdLineItemParsingHandler();
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
    public void verifyVersionMatches() {
        MzTabParser context = Mockito.mock(DummyMzTabParser.class);
        MetaData metaData = Mockito.mock(MetaData.class);
        String fileId = "PXD001386-41337";
        String mztabFileIdLine = "MTD\tmzTab-ID\t" + fileId;
        when(context.getMetaDataSection()).thenReturn(metaData);
        subject.parseLine(context, mztabFileIdLine, 1, 0);
        verify(context, atLeastOnce()).getMetaDataSection();
        verify(metaData, times(1)).getFileId();
        verify(metaData, times(1)).setFileId(fileId);
    }
}