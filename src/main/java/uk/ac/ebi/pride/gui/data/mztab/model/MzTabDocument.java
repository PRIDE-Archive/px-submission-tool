package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidMzTabDocument;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-06-08 23:31
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class MzTabDocument implements Validable {
    private static final Logger logger = LoggerFactory.getLogger(MzTabDocument.class);

    // mzTab Sections
    private MetaData metaData;

    public MzTabDocument() {
        metaData = null;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    @Override
    public boolean validate() throws ValidationException {
        // TODO - Validation criteria
        return false;
    }
}
