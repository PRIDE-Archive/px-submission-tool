package uk.ac.ebi.pride.gui.util;

import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * BorderUtil helps to create borders for Swing components
 *
 * @author Rui Wang
 * @version $Id$
 */
public final class BorderUtil {

    private BorderUtil() {
    }

    /**
     * Create a lowered border using the most appropriate method for the current Java version.
     * This method handles different Java versions and their Nimbus look-and-feel implementations,
     * with special handling for Java 9+ module system.
     */
    public static Border createLoweredBorder() {
        JavaVersion version = JavaVersion.getVersion();

        // For Java 9+, try direct BorderFactory first
        if (version.compareTo(JavaVersion.VERSION_9) >= 0) {
            try {
                return BorderFactory.createLoweredBevelBorder();
            } catch (Exception e) {
                // Fall through to reflection approach
            }
        }

        // Try different border class names based on Java version
        String[] borderClassNames = {
                "javax.swing.plaf.nimbus.LoweredBorder",  // Java 7+
                "com.sun.java.swing.plaf.nimbus.LoweredBorder"  // Older Java versions
        };

        for (String className : borderClassNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?>[] constructors = clazz.getDeclaredConstructors();
                if (constructors.length > 0) {
                    constructors[0].setAccessible(true);
                    return (Border) constructors[0].newInstance();
                }
            } catch (ClassNotFoundException | IllegalAccessException |
                     InstantiationException | InvocationTargetException ex) {
                // Try next class name
                continue;
            }
        }

        // Final fallback to BorderFactory
        return BorderFactory.createLoweredBevelBorder();
    }
}
