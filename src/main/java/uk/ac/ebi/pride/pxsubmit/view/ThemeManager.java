package uk.ac.ebi.pride.pxsubmit.view;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Manages application theming.
 * Applies the PRIDE light theme stylesheet to the scene.
 */
public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    private static final String THEME_LIGHT_CSS = "/css/theme-light.css";

    private static ThemeManager instance;

    private Scene scene;

    private ThemeManager() {}

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void initialize(Scene scene) {
        this.scene = scene;
        applyTheme();
    }

    private void applyTheme() {
        if (scene == null) {
            logger.warn("Cannot apply theme - scene not initialized");
            return;
        }

        // Remove any existing theme stylesheets
        scene.getStylesheets().removeIf(css ->
                css.contains("theme-light") || css.contains("theme-dark") || css.contains("color-"));

        // Add light theme stylesheet
        String cssUrl = Objects.requireNonNull(
                getClass().getResource(THEME_LIGHT_CSS),
                "Theme CSS not found: " + THEME_LIGHT_CSS
        ).toExternalForm();
        scene.getStylesheets().add(cssUrl);
        logger.debug("Applied theme CSS: {}", THEME_LIGHT_CSS);
    }
}
