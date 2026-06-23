package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.ac.ebi.pride.data.model.Contact;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ApiService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.service.SubmissionPxLoader;

/**
 * Login step - First step in the submission wizard.
 * Authenticates user with PRIDE credentials.
 */
public class LoginStep extends AbstractWizardStep {

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private HBox progressBox;
    private Label progressLabel;
    private VBox ticketSelectionBox;
    private ComboBox<String> ticketCombo;
    private Label ticketStatusLabel;
    private uk.ac.ebi.pride.pxsubmit.service.AuthService currentAuth;
    private ApiService ticketApiService;
    private final SubmissionPxLoader submissionPxLoader;
    private CompletableFuture<Void> currentTicketLoad;
    private boolean submissionLoadedFromSelectedTicket;
    private String authenticatedSubmitterName;
    private String authenticatedSubmitterEmail;
    private String authenticatedSubmitterAffiliation;
    private boolean authenticated = false;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public LoginStep(SubmissionModel model) {
        super("login",
              "PRIDE Login",
              "Enter your PRIDE Archive credentials to continue",
              model);
        this.submissionPxLoader = ServiceFactory.getInstance().createSubmissionPxLoader();
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
        errorLabel.setStyle("-fx-text-fill: #dc3545;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Inline progress indicator (shown during authentication)
        progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(20, 20);
        progressLabel = new Label("Authenticating...");
        progressLabel.setStyle("-fx-text-fill: #666;");
        progressBox.getChildren().addAll(pi, progressLabel);
        progressBox.setVisible(false);
        progressBox.setManaged(false);

        ticketSelectionBox = new VBox(8);
        ticketSelectionBox.setAlignment(Pos.CENTER_LEFT);
        ticketSelectionBox.setMaxWidth(360);
        ticketSelectionBox.setVisible(false);
        ticketSelectionBox.setManaged(false);

        Label ticketLabel = new Label("Existing submission tickets:");
        ticketLabel.setStyle("-fx-font-weight: bold;");

        ticketCombo = new ComboBox<>();
        ticketCombo.setPromptText("Select a ticket to resume");
        ticketCombo.setPrefWidth(300);
        ticketCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
            handleTicketSelection(newVal)
        );

        Button clearTicketButton = new Button("Clear");
        clearTicketButton.setOnAction(e -> {
            ticketCombo.getSelectionModel().clearSelection();
        });

        HBox ticketRow = new HBox(8, ticketCombo, clearTicketButton);
        ticketRow.setAlignment(Pos.CENTER_LEFT);

        ticketStatusLabel = new Label("Choose a ticket, or click Next to start a new submission.");
        ticketStatusLabel.setWrapText(true);
        ticketStatusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        ticketSelectionBox.getChildren().addAll(ticketLabel, ticketRow, ticketStatusLabel);

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
            ticketSelectionBox,
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
        hideTicketSelection();

        if (model.isLoggedIn()
                && model.getPendingCheckpoint() == null
                && !model.getAvailableSubmissionTickets().isEmpty()) {
            showTicketSelection(model.getAvailableSubmissionTickets());
        }

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
            return true;
        }

        // If already authenticated, proceed
        if (authenticated) {
            if (isTicketLoadRunning()) {
                ticketStatusLabel.setText("Loading submission.px. Please wait...");
                return false;
            }
            if (model.getSelectedSubmissionTicket() == null || model.getSelectedSubmissionTicket().isBlank()) {
                resetTicketLoadedSubmissionForNewTicket();
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
        hideTicketSelection();
        showProgress("Authenticating...");
        if (wizardController != null) {
            wizardController.setNavigationEnabled(false);
        }

        currentAuth = ServiceFactory.getInstance().createAuthService();
        currentAuth.setCredentials(username, password);

        currentAuth.setOnSucceeded(evt -> {
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

                    rememberAuthenticatedSubmitter(
                        submitterName, contact.getEmail(), contact.getAffiliation());
                } else {
                    rememberAuthenticatedSubmitter(null, username, null);
                    applyAuthenticatedSubmitter(model.getSubmission());
                }
                logger.info("Authentication succeeded for user: {}", username);
                authenticated = true;
                loadSubmissionTickets(username, password);
            } catch (Exception ex) {
                logger.warn("Authentication succeeded but could not process contact details", ex);
                rememberAuthenticatedSubmitter(null, username, null);
                applyAuthenticatedSubmitter(model.getSubmission());
                authenticated = true;
                loadSubmissionTickets(username, password);
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

    private void showProgress(String message) {
        progressLabel.setText(message);
        progressBox.setVisible(true);
        progressBox.setManaged(true);
    }

    private void hideProgress() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
    }

    private void loadSubmissionTickets(String username, String password) {
        showProgress("Loading existing submissions...");

        if (ticketApiService != null) {
            ticketApiService.shutdown();
        }

        ticketApiService = ServiceFactory.getInstance().createApiService(username, password);
        ticketApiService.getSubmissionTickets()
            .thenAccept(tickets -> Platform.runLater(() -> {
                shutdownTicketApiService();
                hideProgress();
                model.setAvailableSubmissionTickets(tickets);

                if (tickets != null && !tickets.isEmpty() && model.getPendingCheckpoint() == null) {
                    showTicketSelection(tickets);
                    if (wizardController != null) {
                        wizardController.setNavigationEnabled(true);
                    }
                } else {
                    continueAfterLogin();
                }
            }))
            .exceptionally(ex -> {
                logger.warn("Could not load existing submission tickets", ex);
                Platform.runLater(() -> {
                    shutdownTicketApiService();
                    hideProgress();
                    model.setAvailableSubmissionTickets(List.of());
                    model.setSelectedSubmissionTicket(null);
                    continueAfterLogin();
                });
                return null;
            });
    }

    private void showTicketSelection(List<String> tickets) {
        String selectedTicket = model.getSelectedSubmissionTicket();
        ticketCombo.getItems().setAll(tickets);
        if (selectedTicket != null && tickets.contains(selectedTicket)) {
            ticketCombo.getSelectionModel().select(selectedTicket);
        } else {
            ticketCombo.getSelectionModel().clearSelection();
            model.setSelectedSubmissionTicket(null);
        }
        ticketStatusLabel.setText("Choose a ticket, or click Next to start a new submission.");
        ticketSelectionBox.setVisible(true);
        ticketSelectionBox.setManaged(true);
    }

    private void handleTicketSelection(String ticketId) {
        model.setSelectedSubmissionTicket(ticketId);

        if (ticketStatusLabel == null) {
            return;
        }

        if (ticketId == null || ticketId.isBlank()) {
            resetTicketLoadedSubmissionForNewTicket();
            ticketStatusLabel.setText("No existing ticket selected. Click Next to create a new submission ticket.");
            return;
        }

        loadSubmissionPxForTicket(ticketId);
    }

    private void loadSubmissionPxForTicket(String ticketId) {
        File submissionFile = submissionPxLoader.resolveSubmissionFile(ticketId);
        if (submissionFile == null) {
            ticketStatusLabel.setText("No submission.px path is configured for ticket " + ticketId + ".");
            return;
        }

        ticketStatusLabel.setText("Loading submission.px for " + ticketId + "...");
        ticketCombo.setDisable(true);
        if (wizardController != null) {
            wizardController.setNavigationEnabled(false);
        }

        CompletableFuture<Void> load = CompletableFuture
            .supplyAsync(() -> {
                try {
                    return submissionPxLoader.load(submissionFile);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            })
            .thenAccept(parsedSubmission ->
                Platform.runLater(() -> applyLoadedSubmission(ticketId, submissionFile, parsedSubmission)))
            .exceptionally(ex -> {
                Platform.runLater(() -> showTicketLoadFailure(ticketId, submissionFile, ex));
                return null;
            });

        currentTicketLoad = load;
        load.whenComplete((ignored, ex) -> Platform.runLater(() -> {
            if (currentTicketLoad == load) {
                ticketCombo.setDisable(false);
                currentTicketLoad = null;
                if (wizardController != null) {
                    wizardController.setNavigationEnabled(true);
                }
            }
        }));
    }

    private void applyLoadedSubmission(String ticketId, File submissionFile, Submission parsedSubmission) {
        if (!ticketId.equals(model.getSelectedSubmissionTicket())) {
            return;
        }

        model.setSubmission(parsedSubmission);
        clearSubmissionProgressState();
        submissionLoadedFromSelectedTicket = true;
        logger.info("Loaded submission metadata for ticket {} from {}", ticketId, submissionFile.getAbsolutePath());
        ticketStatusLabel.setText("Loaded fields from " + submissionFile.getName() + " for " + ticketId + ".");
        if (wizardController != null) {
            wizardController.refreshStepIndicator();
        }
    }

    private void showTicketLoadFailure(String ticketId, File submissionFile, Throwable throwable) {
        if (!ticketId.equals(model.getSelectedSubmissionTicket())) {
            return;
        }

        Throwable cause = unwrapCompletionException(throwable);
        logger.warn("Could not load submission.px for ticket {} from {}", ticketId,
            submissionFile.getAbsolutePath(), cause);
        ticketStatusLabel.setText("Could not load submission.px: " + cause.getMessage());
    }

    private boolean isTicketLoadRunning() {
        return currentTicketLoad != null && !currentTicketLoad.isDone();
    }

    private Throwable unwrapCompletionException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void resetTicketLoadedSubmissionForNewTicket() {
        if (!submissionLoadedFromSelectedTicket) {
            return;
        }

        model.setSubmission(new Submission());
        applyAuthenticatedSubmitter(model.getSubmission());
        clearSubmissionProgressState();
        submissionLoadedFromSelectedTicket = false;
        logger.info("Cleared submission.px metadata because no existing ticket is selected");
        if (wizardController != null) {
            wizardController.refreshStepIndicator();
        }
    }

    private void clearSubmissionProgressState() {
        model.clearChecksums();
        model.getUploadedFiles().clear();
        model.setSummaryFileUploaded(false);
        model.setUploadDetail(null);
        model.getSdrfValidation().clear();
    }

    private void rememberAuthenticatedSubmitter(String name, String email, String affiliation) {
        authenticatedSubmitterName = name;
        authenticatedSubmitterEmail = email;
        authenticatedSubmitterAffiliation = affiliation;
    }

    private void applyAuthenticatedSubmitter(Submission targetSubmission) {
        if (targetSubmission == null || targetSubmission.getProjectMetaData() == null) {
            return;
        }

        Contact submitter = targetSubmission.getProjectMetaData().getSubmitterContact();
        if (submitter == null) {
            submitter = new Contact();
            targetSubmission.getProjectMetaData().setSubmitterContact(submitter);
        }

        submitter.setName(authenticatedSubmitterName);
        submitter.setEmail(authenticatedSubmitterEmail);
        submitter.setAffiliation(authenticatedSubmitterAffiliation);
        submitter.setUserName(model.getUserName());
        String password = passwordField != null ? passwordField.getText() : model.getPassword();
        submitter.setPassword(password != null ? password.toCharArray() : new char[0]);
    }

    private void hideTicketSelection() {
        if (ticketSelectionBox != null) {
            ticketSelectionBox.setVisible(false);
            ticketSelectionBox.setManaged(false);
        }
    }

    private void continueAfterLogin() {
        if (wizardController != null) {
            wizardController.setNavigationEnabled(true);
            if (model.getPendingCheckpoint() != null) {
                logger.info("Resuming from checkpoint - skipping to submission step");
                wizardController.goToStep("submission");
            } else {
                wizardController.goToNextStep();
            }
        }
    }

    private void shutdownTicketApiService() {
        if (ticketApiService != null) {
            ticketApiService.shutdown();
            ticketApiService = null;
        }
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
