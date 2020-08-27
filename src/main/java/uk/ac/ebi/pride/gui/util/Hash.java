package uk.ac.ebi.pride.gui.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Hash {

    private final static Logger LOGGER = LoggerFactory.getLogger(Hash.class);

    public static String getSha1Checksum(File file) throws IOException {

        FileInputStream fileInputStream = new FileInputStream(file);
        String checksum = DigestUtils.sha1Hex(fileInputStream);
        fileInputStream.close();

        return checksum;
    }

    public static String getSha256Checksum(String str) {
        return DigestUtils.sha256Hex(str);
    }
}