package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.util.regex.Pattern;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;

/**
 * Login step - First step in the submission wizard.
 * Authenticates user with PRIDE credentials.
 */
public class LoginStep extends AbstractWizardStep {

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private HBox progressBox;
    private uk.ac.ebi.pride.pxsubmit.service.AuthService currentAuth;
    private boolean authenticated = false;

    private static final String ERROR_STYLE =
        "-fx-text-fill: #842029; "
            + "-fx-background-color: #f8d7da; "
            + "-fx-border-color: #f5c2c7; "
            + "-fx-border-radius: 4; "
            + "-fx-background-radius: 4; "
            + "-fx-padding: 10 12;";
    private static final String NOTICE_STYLE =
        "-fx-text-fill: #0f5132; "
            + "-fx-background-color: #d1e7dd; "
            + "-fx-border-color: #badbcc; "
            + "-fx-border-radius: 4; "
            + "-fx-background-radius: 4; "
            + "-fx-padding: 10 12; "
            + "-fx-font-weight: bold;";
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public LoginStep(SubmissionModel model) {
        super("login",
              "PRIDE Login",
              "Enter your PRIDE Archive credentials to continue",
              model);
    }

    @Override
    protected Parent createContent() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setMaxWidth(500);

        // User icon
        Label loginIcon = new Label("\uD83D\uDC64"); // Bust silhouette icon
        loginIcon.setStyle("-fx-font-size: 48px;");

        // Login form
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(15);
        form.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Email:");
        usernameField = new TextField();
        usernameField.setPromptText("your.email@example.com");
        usernameField.setPrefWidth(300);

        Label passwordLabel = new Label("Password:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefWidth(300);

        form.add(usernameLabel, 0, 0);
        form.add(usernameField, 1, 0);
        form.add(passwordLabel, 0, 1);
        form.add(passwordField, 1, 1);

        // Error label
        errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(420);
        errorLabel.setPrefWidth(420);
        errorLabel.setMinHeight(Control.USE_PREF_SIZE);
        errorLabel.setLineSpacing(2);
        errorLabel.setStyle(ERROR_STYLE);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Inline progress indicator (shown during authentication)
        progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(20, 20);
        Label progressLabel = new Label("Authenticating...");
        progressLabel.setStyle("-fx-text-fill: #666;");
        progressBox.getChildren().addAll(pi, progressLabel);
        progressBox.setVisible(false);
        progressBox.setManaged(false);

        // Register link
        Hyperlink registerLink = new Hyperlink("Register for a PRIDE account");
        registerLink.setOnAction(e -> openRegistrationPage());

        // Forgot password link
        Hyperlink forgotLink = new Hyperlink("Forgot password?");
        forgotLink.setOnAction(e -> openForgotPasswordPage());

        HBox linksBox = new HBox(20);
        linksBox.setAlignment(Pos.CENTER);
        linksBox.getChildren().addAll(registerLink, forgotLink);

        root.getChildren().addAll(
            loginIcon,
            form,
            errorLabel,
            progressBox,
            linksBox
        );

        // Wrap in a scroll pane so the form stays reachable (rather than being
        // clipped) on very short windows, while remaining centered when there's room.
        VBox centeringWrapper = new VBox(root);
        centeringWrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(centeringWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        return scrollPane;
    }

    @Override
    public boolean canSkip() {
        // Skip login entirely in test mode
        if (model.isTrainingMode()) {
            model.setLoggedIn(true);
            return true;
        }
        return false;
    }

    @Override
    protected void initializeStep() {
        // Bind validation - valid when both fields have content
        valid.bind(
            usernameField.textProperty().isNotEmpty()
            .and(passwordField.textProperty().isNotEmpty())
        );

        // Bind to model
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            model.setUserName(newVal);
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            model.setPassword(newVal);
        });

        // Let Enter in login fields behave like clicking Next.
        usernameField.setOnAction(event -> triggerNextFromEnter());
        passwordField.setOnAction(event -> triggerNextFromEnter());

        // Pre-fill from model if available
        if (model.getUserName() != null) {
            usernameField.setText(model.getUserName());
        }
    }

    private void triggerNextFromEnter() {
        if (wizardController != null && valid.get()) {
            wizardController.triggerNext();
        }
    }

    @Override
    protected void onStepEntering() {
        // Clear any previous error
        hideError();

        // Test mode - show notice that login is not required
        if (model.isTrainingMode()) {
            showTestModeNotice();
            return;
        }

        usernameField.setText(model.getUserName() != null ? model.getUserName() : "");

        // Focus username field
        usernameField.requestFocus();

        // Reset auth state if coming back to this step (e.g. after logout)
        if (!model.isLoggedIn()) {
            authenticated = false;
            passwordField.clear();
        }
    }

    private void showTestModeNotice() {
        errorLabel.setText("Test Mode: Login not required. Click 'Next' to continue.");
        errorLabel.setStyle(NOTICE_STYLE);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @Override
    public boolean validate() {
        // Test mode - skip login completely, no credentials needed
        if (model.isTrainingMode()) {
            logger.info("Test mode - skipping authentication, no login required");
            model.setLoggedIn(true);
            return true;
        }

        // If already authenticated, proceed
        if (authenticated) {
            return true;
        }

        // Basic validation
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            showError("Please enter your email address");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(username).matches()) {
            showError("Please enter a valid email address");
            return false;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            return false;
        }

        // Cancel any previous auth attempt
        if (currentAuth != null && currentAuth.isRunning()) {
            currentAuth.cancel();
        }

        // Disable navigation and show inline progress
        hideError();
        showProgress();
        if (wizardController != null) {
            wizardController.setNavigationEnabled(false);
        }

        currentAuth = ServiceFactory.getInstance().createAuthService();
        currentAuth.setCredentials(username, password);

        currentAuth.setOnSucceeded(evt -> {
            hideProgress();
            try {
                uk.ac.ebi.pride.pxsubmit.service.AuthService.AuthenticationResult authResult = currentAuth.getValue();
                model.setLoggedIn(true);
                model.setAuthToken(null);
                uk.ac.ebi.pride.archive.submission.model.user.ContactDetail contact =
                    authResult != null ? authResult.getContactDetail() : null;

                uk.ac.ebi.pride.data.model.Contact submitter =
                    model.getSubmission().getProjectMetaData().getSubmitterContact();
                if (submitter == null) {
                    submitter = new uk.ac.ebi.pride.data.model.Contact();
                    model.getSubmission().getProjectMetaData().setSubmitterContact(submitter);
                }

                if (contact != null) {
                    // Build submitter name from first + last name
                    String submitterName = "";
                    if (contact.getFirstName() != null) submitterName = contact.getFirstName();
                    if (contact.getLastName() != null) submitterName += " " + contact.getLastName();
                    submitterName = submitterName.trim();

                    submitter.setName(submitterName);
                    submitter.setEmail(contact.getEmail() != null ? contact.getEmail() : username);
                    submitter.setAffiliation(contact.getAffiliation());
                } else {
                    submitter.setEmail(username);
                }
                submitter.setUserName(username);
                submitter.setPassword(passwordField.getText().toCharArray());
                logger.info("Authentication succeeded for user: {}", username);
                authenticated = true;

                // Auto-advance: skip to upload step if resuming from checkpoint
                if (wizardController != null) {
                    wizardController.setNavigationEnabled(true);
                    if (model.getPendingCheckpoint() != null) {
                        logger.info("Resuming from checkpoint - skipping to submission step");
                        wizardController.goToStep("submission");
                    } else {
                        wizardController.goToNextStep();
                    }
                }
            } catch (Exception ex) {
                logger.warn("Authentication succeeded but could not process contact details", ex);
                authenticated = true;
                if (wizardController != null) {
                    wizardController.setNavigationEnabled(true);
                    if (model.getPendingCheckpoint() != null) {
                        wizardController.goToStep("submission");
                    } else {
                        wizardController.goToNextStep();
                    }
                }
            }
        });

        currentAuth.setOnFailed(evt -> {
            hideProgress();
            Throwable ex = currentAuth.getException();
            String msg = ex != null ? ex.getMessage() : "Authentication failed";
            showError(msg);
            if (wizardController != null) {
                wizardController.setNavigationEnabled(true);
            }
        });

        currentAuth.start();

        // Return false to prevent immediate navigation; navigation continues in the succeeded handler.
        return false;
    }

    @Override
    public boolean showBackButton() {
        return true; // Allow going back to welcome page
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle(ERROR_STYLE);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showProgress() {
        progressBox.setVisible(true);
        progressBox.setManaged(true);
    }

    private void hideProgress() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
    }

    private void openRegistrationPage() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.ebi.ac.uk/pride/register"));
        } catch (Exception e) {
            logger.warn("Could not open registration page", e);
        }
    }

    private void openForgotPasswordPage() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.ebi.ac.uk/pride/forgotpassword"));
        } catch (Exception e) {
            logger.warn("Could not open forgot password page", e);
        }
    }
}
