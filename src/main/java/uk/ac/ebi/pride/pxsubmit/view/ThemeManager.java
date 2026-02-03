package uk.ac.ebi.pride.pxsubmit.view;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Manages application theming with support for light and dark modes.
 *
 * Features:
 * - Theme switching at runtime
 * - Persistent theme preference storage
 * - System theme detection (optional)
 * - Observable theme property for UI binding
 */
public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    private static final String PREF_KEY_THEME = "app.theme";
    private static final String THEME_LIGHT_CSS = "/css/theme-light.css";
    private static final String THEME_DARK_CSS = "/css/theme-dark.css";

    private static ThemeManager instance;

    private final Preferences preferences;
    private final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>(Theme.LIGHT);
    private Scene scene;

    /**
     * Available themes
     */
    public enum Theme {
        LIGHT("Light", THEME_LIGHT_CSS),
        DARK("Dark", THEME_DARK_CSS),
        SYSTEM("System", null); // Follow system preference

        private final String displayName;
        private final String cssPath;

        Theme(String displayName, String cssPath) {
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssPath() {
            return cssPath;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private ThemeManager() {
        preferences = Preferences.userNodeForPackage(ThemeManager.class);
        loadSavedTheme();
    }

    /**
     * Get singleton instance
     */
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Initialize with scene
     */
    public void initialize(Scene scene) {
        this.scene = scene;
        applyTheme(currentTheme.get());
    }

    /**
     * Get current theme property (for binding)
     */
    public ObjectProperty<Theme> currentThemeProperty() {
        return currentTheme;
    }

    /**
     * Get current theme
     */
    public Theme getCurrentTheme() {
        return currentTheme.get();
    }

    /**
     * Set theme
     */
    public void setTheme(Theme theme) {
        if (theme == null) {
            theme = Theme.LIGHT;
        }

        currentTheme.set(theme);
        applyTheme(theme);
        saveTheme(theme);

        logger.info("Theme changed to: {}", theme.getDisplayName());
    }

    /**
     * Toggle between light and dark themes
     */
    public void toggleTheme() {
        Theme current = currentTheme.get();
        Theme newTheme = (current == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        setTheme(newTheme);
    }

    /**
     * Check if current theme is dark
     */
    public boolean isDarkTheme() {
        Theme theme = currentTheme.get();
        if (theme == Theme.SYSTEM) {
            return isSystemDarkMode();
        }
        return theme == Theme.DARK;
    }

    /**
     * Apply theme to scene
     */
    private void applyTheme(Theme theme) {
        if (scene == null) {
            logger.warn("Cannot apply theme - scene not initialized");
            return;
        }

        // Determine effective theme
        Theme effectiveTheme = theme;
        if (theme == Theme.SYSTEM) {
            effectiveTheme = isSystemDarkMode() ? Theme.DARK : Theme.LIGHT;
        }

        // Remove existing theme stylesheets
        scene.getStylesheets().removeIf(css ->
                css.contains("theme-light") || css.contains("theme-dark"));

        // Add new theme stylesheet
        String cssPath = effectiveTheme.getCssPath();
        if (cssPath != null) {
            String cssUrl = Objects.requireNonNull(
                    getClass().getResource(cssPath),
                    "Theme CSS not found: " + cssPath
            ).toExternalForm();

            scene.getStylesheets().add(cssUrl);
            logger.debug("Applied theme CSS: {}", cssPath);
        }
    }

    /**
     * Detect system dark mode preference
     */
    private boolean isSystemDarkMode() {
        // Check macOS dark mode
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("mac")) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "defaults", "read", "-g", "AppleInterfaceStyle"
                );
                Process process = pb.start();
                int exitCode = process.waitFor();
                return exitCode == 0; // Returns 0 if "Dark" is set
            } catch (Exception e) {
                logger.debug("Could not detect macOS dark mode", e);
            }
        }

        // Check Windows dark mode
        if (osName.contains("windows")) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "reg", "query",
                        "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme"
                );
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                return output.contains("0x0"); // 0 means dark mode
            } catch (Exception e) {
                logger.debug("Could not detect Windows dark mode", e);
            }
        }

        // Default to light
        return false;
    }

    /**
     * Save theme preference
     */
    private void saveTheme(Theme theme) {
        preferences.put(PREF_KEY_THEME, theme.name());
        try {
            preferences.flush();
        } catch (Exception e) {
            logger.warn("Failed to save theme preference", e);
        }
    }

    /**
     * Load saved theme preference
     */
    private void loadSavedTheme() {
        String savedTheme = preferences.get(PREF_KEY_THEME, Theme.LIGHT.name());
        try {
            currentTheme.set(Theme.valueOf(savedTheme));
        } catch (IllegalArgumentException e) {
            currentTheme.set(Theme.LIGHT);
        }
        logger.debug("Loaded theme preference: {}", currentTheme.get());
    }

    /**
     * Get CSS class for current theme (useful for conditional styling)
     */
    public String getThemeClass() {
        return isDarkTheme() ? "dark-theme" : "light-theme";
    }

    /**
     * Add theme listener
     */
    public void addThemeChangeListener(Runnable listener) {
        currentTheme.addListener((obs, oldVal, newVal) -> listener.run());
    }
}
