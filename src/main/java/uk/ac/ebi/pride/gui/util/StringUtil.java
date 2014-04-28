package uk.ac.ebi.pride.gui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for handling string
 *
 * @author Rui Wang
 * @version $Id$
 */
public final class StringUtil {
    public static final String COMMA = ",";
    public static final String COMMA_WHITESPACE_SPLIT_PATTERN = "[, ]+";

    private StringUtil() {
    }

    /**
     * Concatenate a list of strings
     *
     * @param stringList given list of strings
     * @return String  concatenated string
     */
    public static String concatenateStrings(Collection<String> stringList) {
        StringBuilder content = new StringBuilder();
        for (String str : stringList) {
            content.append(str);
            content.append(COMMA);
        }
        String contentStr = content.toString();
        if (contentStr.length() > 0) {
            contentStr = contentStr.substring(0, contentStr.lastIndexOf(COMMA));
        }

        return contentStr;
    }

    /**
     * Split a string into list using given regular expression
     *
     * @param str   given string
     * @param regex given regular expression
     * @return List<String>    a list of string tokens
     */
    public static List<String> splitString(String str, String regex) {
        java.util.List<String> tokens = new ArrayList<String>();

        if (str != null) {
            str = str.trim();
            if (str.length() > 0) {
                String[] parts = str.split(regex);
                tokens.addAll(Arrays.asList(parts));
            }
        }

        return tokens;
    }
}
