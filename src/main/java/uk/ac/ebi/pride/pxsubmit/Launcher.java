package uk.ac.ebi.pride.pxsubmit;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Launcher class for the PX Submission Tool.
 *
 * This class is needed because JavaFX applications packaged in fat JARs
 * cannot have a main class that extends javafx.application.Application.
 * The Java launcher checks for JavaFX runtime before loading such classes.
 *
 * This launcher acts as an intermediary that doesn't extend Application,
 * allowing the JAR to be executed normally.
 *
 * It also bootstraps the correct JavaFX native libraries for the running
 * OS/CPU so a single (universal) fat JAR can run natively on Windows x64,
 * Linux x64/arm64 and macOS x64/arm64 without relying on emulation. See
 * {@link #setupJavaFxNatives()} for details. This class must NOT reference
 * any JavaFX type, so that the natives are installed before JavaFX's own
 * native loader runs.
 */
public class Launcher {

    public static void main(String[] args) {
        try {
            setupJavaFxNatives();
        } catch (Throwable t) {
            // Non-fatal: in a dev build (no bundled per-platform natives) JavaFX
            // resolves its natives the normal way. Log and continue.
            System.err.println("[pxsubmit] JavaFX native bootstrap skipped: " + t);
        }
        PxSubmitApplication.main(args);
    }

    /**
     * Extracts the JavaFX native libraries matching the current OS/CPU from the
     * jar (bundled under {@code natives/<platform>/}) into a temp directory and
     * prepends that directory to {@code java.library.path}.
     *
     * JavaFX's {@code com.sun.glass.utils.NativeLibLoader} reads
     * {@code java.library.path} live at load time, so as long as the matching
     * natives are removed from the jar root (they would otherwise be picked by
     * arch-agnostic file name and could be the wrong CPU) this directs JavaFX to
     * load the correct architecture. Co-locating every lib for the platform in
     * one directory lets the dynamic linker resolve inter-library dependencies
     * via {@code @loader_path}/rpath, mirroring JavaFX's own cache mechanism.
     *
     * If no bundled natives are found (e.g. a plain dev build), this is a no-op.
     */
    private static void setupJavaFxNatives() throws Exception {
        String platform = detectPlatform();
        if (platform == null) {
            return;
        }
        String prefix = "natives/" + platform + "/";

        URL location = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
        if (location == null) {
            return;
        }
        File source = new File(location.toURI());

        Path tempDir = Files.createTempDirectory("pxsubmit-javafx");
        tempDir.toFile().deleteOnExit();
        boolean extracted = false;

        if (source.isFile()) {
            try (JarFile jar = new JarFile(source)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                        continue;
                    }
                    String fileName = entry.getName().substring(prefix.length());
                    if (fileName.isEmpty() || fileName.contains("/")) {
                        continue;
                    }
                    File out = new File(tempDir.toFile(), fileName);
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    out.deleteOnExit();
                    extracted = true;
                }
            }
        } else if (source.isDirectory()) {
            File dir = new File(source, prefix);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isFile()) {
                        continue;
                    }
                    File out = new File(tempDir.toFile(), f.getName());
                    Files.copy(f.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    out.deleteOnExit();
                    extracted = true;
                }
            }
        }

        if (!extracted) {
            return;
        }

        String existing = System.getProperty("java.library.path", "");
        String updated = existing.isEmpty()
                ? tempDir.toAbsolutePath().toString()
                : tempDir.toAbsolutePath() + File.pathSeparator + existing;
        System.setProperty("java.library.path", updated);
    }

    /**
     * Maps the running OS/CPU to the bundled natives folder name. Returns
     * {@code null} for unknown platforms (let JavaFX fall back to its defaults).
     * Windows always maps to the x64 bundle (no official JavaFX win-arm64 build;
     * Windows-on-ARM runs an x64 JRE under emulation).
     */
    private static String detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean arm64 = arch.contains("aarch64") || arch.contains("arm64");

        if (os.contains("win")) {
            return "win";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return arm64 ? "mac-aarch64" : "mac";
        }
        if (os.contains("nux") || os.contains("nix")) {
            return arm64 ? "linux-aarch64" : "linux";
        }
        return null;
    }
}
