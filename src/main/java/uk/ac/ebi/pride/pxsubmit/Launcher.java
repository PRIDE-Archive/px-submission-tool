package uk.ac.ebi.pride.pxsubmit;

/**
 * Launcher class for the PX Submission Tool.
 *
 * This class is needed because JavaFX applications packaged in fat JARs
 * cannot have a main class that extends javafx.application.Application.
 * The Java launcher checks for JavaFX runtime before loading such classes.
 *
 * This launcher acts as an intermediary that doesn't extend Application,
 * allowing the JAR to be executed normally.
 */
public class Launcher {

    public static void main(String[] args) {
        PxSubmitApplication.main(args);
    }
}
