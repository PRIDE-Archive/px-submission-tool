package uk.ac.ebi.pride.gui.util;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;

/**
 * Search engine output supported by PRIDE Converter
 *
 * @author Rui Wang
 * @version $Id$
 */
public final class PrideConverterSupport {

    private PrideConverterSupport() {
    }

    public static boolean isSupported(DataFile dataFile) {
        MassSpecFileFormat fileFormat = dataFile.getFileFormat();
        if (fileFormat != null && fileFormat.getFileType().equals(ProjectFileType.SEARCH) &&
                (fileFormat.equals(MassSpecFileFormat.DAT) ||
                        fileFormat.equals(MassSpecFileFormat.MZIDENTML) ||
                        fileFormat.equals(MassSpecFileFormat.OMSSA_OMX) ||
                        fileFormat.equals(MassSpecFileFormat.MSGF) ||
                        fileFormat.equals(MassSpecFileFormat.SPECTRAST) ||
                        fileFormat.equals(MassSpecFileFormat.XTANDEM) ||
                        fileFormat.equals(MassSpecFileFormat.CRUX) ||
                        fileFormat.equals(MassSpecFileFormat.CSV) ||
                        fileFormat.equals(MassSpecFileFormat.TSV))) {
            return true;
        } else {
            return false;
        }
    }
}
