package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

/**
 * Login step - First step in the submission wizard.
 * Authenticates user with PRIDE credentials.
 */
public class LoginStep extends AbstractWizardStep {

    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox trainingModeCheckbox;
    private Label errorLabel;
    private HBox progressBox;
    private uk.ac.ebi.pride.pxsubmit.service.AuthService currentAuth;

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

        // Welcome text
        Label welcomeLabel = new Label("Welcome to the PX Submission Tool");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label instructionLabel = new Label(
            "Please log in with your PRIDE Archive account.\n" +
            "If you don't have an account, register at pride.ebi.ac.uk"
        );
        instructionLabel.setWrapText(true);
        instructionLabel.setStyle("-fx-text-fill: #666;");

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

        // Training mode checkbox
        trainingModeCheckbox = new CheckBox("Training Mode (no files will be uploaded)");
        trainingModeCheckbox.selectedProperty().bindBidirectional(model.trainingModeProperty());

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #dc3545;");
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
            welcomeLabel,
            instructionLabel,
            form,
            trainingModeCheckbox,
            errorLabel,
            progressBox,
            linksBox
        );

        return root;
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

        // Pre-fill from model if available
        if (model.getUserName() != null) {
            usernameField.setText(model.getUserName());
        }
    }

    @Override
    protected void onStepEntering() {
        // Focus username field
        usernameField.requestFocus();

        // Clear any previous error
        hideError();
    }

    @Override
    public boolean validate() {
        // Basic validation
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            showError("Please enter your email address");
            return false;
        }

        if (!username.contains("@")) {
            showError("Please enter a valid email address");
            return false;
        }

        if (password.isEmpty()) {
            showError("Please enter your password");
            return false;
        }

        // Training mode - skip actual authentication
        if (model.isTrainingMode()) {
            logger.info("Training mode - skipping authentication");
            model.setLoggedIn(true);
            return true;
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

        currentAuth = new uk.ac.ebi.pride.pxsubmit.service.AuthService();
        currentAuth.setCredentials(username, password);

        currentAuth.setOnSucceeded(evt -> {
            hideProgress();
            try {
                uk.ac.ebi.pride.archive.submission.model.user.ContactDetail contact = currentAuth.getValue();
                model.setLoggedIn(true);
                if (contact != null) {
                    // Build submitter name from first + last name
                    String submitterName = "";
                    if (contact.getFirstName() != null) submitterName = contact.getFirstName();
                    if (contact.getLastName() != null) submitterName += " " + contact.getLastName();
                    submitterName = submitterName.trim();

                    // Set submitter contact on the Submission object
                    uk.ac.ebi.pride.data.model.Contact submitter =
                        model.getSubmission().getProjectMetaData().getSubmitterContact();
                    if (submitter == null) {
                        submitter = new uk.ac.ebi.pride.data.model.Contact();
                        model.getSubmission().getProjectMetaData().setSubmitterContact(submitter);
                    }
                    submitter.setName(submitterName);
                    submitter.setEmail(contact.getEmail());
                    submitter.setAffiliation(contact.getAffiliation());
                    submitter.setUserName(username);
                    submitter.setPassword(passwordField.getText().toCharArray());
                }
                logger.info("Authentication succeeded for user: {}", username);
            } catch (Exception ex) {
                logger.warn("Authentication succeeded but could not process contact details", ex);
            } finally {
                if (wizardController != null) {
                    wizardController.setNavigationEnabled(true);
                    wizardController.goToNextStep();
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
        return false; // First step has no back button
    }

    private void showError(String message) {
        errorLabel.setText(message);
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
        // TODO: Open browser to registration page
        logger.info("Opening registration page");
    }

    private void openForgotPasswordPage() {
        // TODO: Open browser to forgot password page
        logger.info("Opening forgot password page");
    }
}
