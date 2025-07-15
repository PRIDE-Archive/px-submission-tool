package uk.ac.ebi.pride.gui.util;

/**
 * Check the local Java version
 *
 * @author Rui Wang
 * @version $Id$
 */
public enum JavaVersion {
    VERSION_1,
    VERSION_2,
    VERSION_3,
    VERSION_4,
    VERSION_5,
    VERSION_6,
    VERSION_7,
    VERSION_8,
    VERSION_9,
    VERSION_10,
    VERSION_11,
    VERSION_12,
    VERSION_13,
    VERSION_14,
    VERSION_15,
    VERSION_16,
    VERSION_17,
    VERSION_18,
    VERSION_19,
    VERSION_20,
    VERSION_21,
    VERSION_UNKNOWN;

    public static JavaVersion getVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("21")) {
            return VERSION_21;
        } else if (version.startsWith("20")) {
            return VERSION_20;
        } else if (version.startsWith("19")) {
            return VERSION_19;
        } else if (version.startsWith("18")) {
            return VERSION_18;
        } else if (version.startsWith("17")) {
            return VERSION_17;
        } else if (version.startsWith("16")) {
            return VERSION_16;
        } else if (version.startsWith("15")) {
            return VERSION_15;
        } else if (version.startsWith("14")) {
            return VERSION_14;
        } else if (version.startsWith("13")) {
            return VERSION_13;
        } else if (version.startsWith("12")) {
            return VERSION_12;
        } else if (version.startsWith("11")) {
            return VERSION_11;
        } else if (version.startsWith("10")) {
            return VERSION_10;
        } else if (version.startsWith("9")) {
            return VERSION_9;
        } else if (version.startsWith("1.8")) {
            return VERSION_8;
        } else if (version.startsWith("1.7")) {
            return VERSION_7;
        } else if (version.startsWith("1.6")) {
            return VERSION_6;
        } else if (version.startsWith("1.5")) {
            return VERSION_5;
        } else if (version.startsWith("1.4")) {
            return VERSION_4;
        } else if (version.startsWith("1.3")) {
            return VERSION_3;
        } else if (version.startsWith("1.2")) {
            return VERSION_2;
        } else if (version.startsWith("1.1")) {
            return VERSION_1;
        } else {
            return VERSION_UNKNOWN;
        }
    }
}
