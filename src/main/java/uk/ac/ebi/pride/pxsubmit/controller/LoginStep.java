package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetail;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ApiService;
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

    // Resubmission UI (shown after successful authentication)
    private VBox resubmissionSection;
    private CheckBox resubmissionCheckbox;
    private ComboBox<String> projectComboBox;
    private HBox projectLoadingBox;
    private Label resubmissionErrorLabel;
    private boolean authenticated = false;

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

        // Welcome text
        Label welcomeLabel = new Label("Welcome to the PX Submission Tool");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

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

        // Resubmission section (hidden until auth succeeds)
        resubmissionSection = createResubmissionSection();
        resubmissionSection.setVisible(false);
        resubmissionSection.setManaged(false);

        root.getChildren().addAll(
            welcomeLabel,
            form,
            errorLabel,
            progressBox,
            linksBox,
            resubmissionSection
        );

        return root;
    }

    private VBox createResubmissionSection() {
        VBox section = new VBox(10);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setMaxWidth(400);
        section.setStyle(
            "-fx-background-color: #f0f7ff; " +
            "-fx-border-color: #b3d4fc; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 15;");

        Label authSuccessLabel = new Label("Logged in successfully!");
        authSuccessLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");

        resubmissionCheckbox = new CheckBox("Resubmit to an existing project");
        resubmissionCheckbox.setStyle("-fx-font-weight: bold;");

        // Project selection (hidden until checkbox checked)
        VBox projectSelectionBox = new VBox(8);
        projectSelectionBox.setPadding(new Insets(0, 0, 0, 25));

        Label projectLabel = new Label("Select project:");
        projectComboBox = new ComboBox<>();
        projectComboBox.setPromptText("Loading projects...");
        projectComboBox.setPrefWidth(300);
        projectComboBox.setDisable(true);

        projectLoadingBox = new HBox(8);
        projectLoadingBox.setAlignment(Pos.CENTER_LEFT);
        ProgressIndicator projectSpinner = new ProgressIndicator();
        projectSpinner.setPrefSize(16, 16);
        Label loadingLabel = new Label("Loading your projects...");
        loadingLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        projectLoadingBox.getChildren().addAll(projectSpinner, loadingLabel);
        projectLoadingBox.setVisible(false);
        projectLoadingBox.setManaged(false);

        resubmissionErrorLabel = new Label();
        resubmissionErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        resubmissionErrorLabel.setVisible(false);
        resubmissionErrorLabel.setManaged(false);

        projectSelectionBox.getChildren().addAll(projectLabel, projectComboBox, projectLoadingBox, resubmissionErrorLabel);
        projectSelectionBox.setVisible(false);
        projectSelectionBox.setManaged(false);

        // Toggle project selection visibility when checkbox changes
        resubmissionCheckbox.selectedProperty().addListener((obs, oldVal, selected) -> {
            projectSelectionBox.setVisible(selected);
            projectSelectionBox.setManaged(selected);
            if (selected) {
                loadProjects();
            } else {
                model.setResubmissionMode(false);
                model.getSubmission().getProjectMetaData().setResubmissionPxAccession(null);
                projectComboBox.getItems().clear();
            }
        });

        Label hintLabel = new Label("Click 'Next' to continue");
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        section.getChildren().addAll(authSuccessLabel, resubmissionCheckbox, projectSelectionBox, hintLabel);
        return section;
    }

    private void loadProjects() {
        projectLoadingBox.setVisible(true);
        projectLoadingBox.setManaged(true);
        projectComboBox.setDisable(true);
        projectComboBox.getItems().clear();
        resubmissionErrorLabel.setVisible(false);
        resubmissionErrorLabel.setManaged(false);

        ApiService apiService = ServiceFactory.getInstance().createApiService(model.getUserName(), model.getPassword());
        apiService.getSubmissionDetails()
            .thenAccept(projectDetailList -> Platform.runLater(() -> {
                projectLoadingBox.setVisible(false);
                projectLoadingBox.setManaged(false);

                if (projectDetailList.getProjectDetails().isEmpty()) {
                    resubmissionErrorLabel.setText("No private projects found, or a resubmission is already pending.");
                    resubmissionErrorLabel.setVisible(true);
                    resubmissionErrorLabel.setManaged(true);
                    projectComboBox.setPromptText("No projects available");
                } else {
                    for (ProjectDetail pd : projectDetailList.getProjectDetails()) {
                        projectComboBox.getItems().add(pd.getAccession());
                    }
                    projectComboBox.setPromptText("Select a project...");
                    projectComboBox.setDisable(false);
                }
                apiService.shutdown();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    projectLoadingBox.setVisible(false);
                    projectLoadingBox.setManaged(false);
                    String message = buildResubmissionErrorMessage(ex);
                    resubmissionErrorLabel.setText("Failed to load projects: " + message);
                    resubmissionErrorLabel.setVisible(true);
                    resubmissionErrorLabel.setManaged(true);
                });
                apiService.shutdown();
                return null;
            });
    }

    /**
     * Build a user-friendly error message for resubmission load failure.
     * Unwraps CompletionException and handles class-loading errors (e.g. NoClassDefFoundError).
     */
    private static String buildResubmissionErrorMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause != null ? cause.getMessage() : ex.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = cause != null ? cause.getClass().getSimpleName() : ex.getClass().getSimpleName();
        }
        // If the message looks like a class name (e.g. NoClassDefFoundError), show a clearer hint
        if (cause instanceof NoClassDefFoundError || cause instanceof ClassNotFoundException
                || (msg != null && msg.contains(".") && !msg.contains(" ") && msg.length() > 20)) {
            return "A required component could not be loaded. Please run the application from the full distribution (extract the zip and use start.bat/start.sh). If the problem persists, check the log file.";
        }
        return msg;
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
        // Clear any previous error
        hideError();

        // Test mode - show notice that login is not required
        if (model.isTrainingMode()) {
            showTestModeNotice();
            return;
        }

        // Focus username field
        usernameField.requestFocus();

        // Reset auth state if coming back to this step
        if (!model.isLoggedIn()) {
            authenticated = false;
            resubmissionSection.setVisible(false);
            resubmissionSection.setManaged(false);
        }
    }

    private void showTestModeNotice() {
        errorLabel.setText("Test Mode: Login not required. Click 'Next' to continue.");
        errorLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @Override
    public boolean validate() {
        // Test mode - skip login completely, no credentials needed
        if (model.isTrainingMode()) {
            logger.info("Test mode - skipping authentication, no login required");
            model.setLoggedIn(true);
            model.setResubmissionMode(false);
            return true;
        }

        // If already authenticated, handle resubmission selection and proceed
        if (authenticated) {
            if (resubmissionCheckbox.isSelected()) {
                String selectedAccession = projectComboBox.getValue();
                if (selectedAccession == null || selectedAccession.isEmpty()) {
                    showError("Please select a project for resubmission");
                    return false;
                }
                model.setResubmissionMode(true);
                model.getSubmission().getProjectMetaData().setResubmissionPxAccession(selectedAccession);
                logger.info("Resubmission mode enabled for project: {}", selectedAccession);
            } else {
                model.setResubmissionMode(false);
                model.getSubmission().getProjectMetaData().setResubmissionPxAccession(null);
            }
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
                authenticated = true;

                // Show resubmission section and let user choose before proceeding
                resubmissionSection.setVisible(true);
                resubmissionSection.setManaged(true);
            } catch (Exception ex) {
                logger.warn("Authentication succeeded but could not process contact details", ex);
                authenticated = true;
            } finally {
                if (wizardController != null) {
                    wizardController.setNavigationEnabled(true);
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
