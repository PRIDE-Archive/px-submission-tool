package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidMzTabDocument;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-08 23:52
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class MzTabFullDocumentParser extends MzTabParser {
    private static final Logger logger = LoggerFactory.getLogger(MzTabFullDocumentParser.class);

    // mzTab source
    private String fileUrl;

    public MzTabFullDocumentParser(String fileUrl) {
        super();
        this.fileUrl = fileUrl;
        // TODO - Set initial state
    }

    @Override
    protected void doParse() {
        // TODO
    }

    @Override
    protected void doValidateProduct() throws InvalidMzTabDocument {
        // Validate the MzTabDocument
        getMzTabDocument().validate();
    }
}
