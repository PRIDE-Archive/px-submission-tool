package uk.ac.ebi.pride.gui.util;

/**
 * Created by ilias on 02/05/14 for.detectOS project
 */
public final class OSDetector {
    public static final String os = System.getProperty("os.name").toLowerCase();
    public static final String arch = System.getProperty("os.arch");

    public enum OS {
        LINUX_64,
        LINUX_32,
        MAC,
        WINDOWS_64,
        WINDOWS_32,
        UNSUPPORTED
    }

    public static OS getOS() {
        if (os.contains("linux")) {
            if (arch.contains("amd64")) {
                return OS.LINUX_64;
            } else {
                return OS.LINUX_32;
            }
        } else if (os.contains("mac")) {
            return OS.MAC;
        } else if (os.contains("win")) {
            if (arch.contains("64")) {
                return OS.WINDOWS_64;
            } else {
                return OS.WINDOWS_32;
            }
        } else {
            return OS.UNSUPPORTED;
        }
    }
}
