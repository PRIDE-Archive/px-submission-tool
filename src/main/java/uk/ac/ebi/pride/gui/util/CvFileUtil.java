package uk.ac.ebi.pride.gui.util;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.gui.data.ExtendedCvParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author Rui Wang
 * @version $Id$
 */
public final class CvFileUtil {

    private CvFileUtil() {
    }

    public static Collection<String> parseByLine(String file) throws IOException {
        // NOTE - TODO - Is it important to to preserve the insertion order?
        Collection<String> values = new LinkedHashSet<>();

        InputStream defaultInstrumentStream = CvFileUtil.class.getClassLoader().getResourceAsStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(defaultInstrumentStream));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!"".equals(line)) {
                values.add(line);
            }
        }

        return values;
    }

    public static Collection<CvParam> parseByTabDelimitedLine(String defaultValueFile) throws IOException {
        Collection<CvParam> values = new LinkedHashSet<>();

        InputStream defaultInstrumentStream = CvFileUtil.class.getClassLoader().getResourceAsStream(defaultValueFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(defaultInstrumentStream));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!"".equals(line)) {
                String[] parts = line.split("\t");
                if (parts.length >= 4) {
                    CvParam cvParam = new ExtendedCvParam(parts[0], parts[1], parts[2], parts[3], null);
                    values.add(cvParam);
                }
            }
        }

        return values;
    }
}
