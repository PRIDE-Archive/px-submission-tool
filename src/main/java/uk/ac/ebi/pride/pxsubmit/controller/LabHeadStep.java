package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

import javafx.application.Platform;

/**
 * Step for collecting Lab Head / Principal Investigator information.
 *
 * Fields:
 * - Name (required)
 * - Email (required)
 * - Affiliation (required)
 * - ORCID iD (optional)
 */
public class LabHeadStep extends AbstractWizardStep {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ORCID_PATTERN =
            Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$");

    private static final String ORCID_API_URL = "https://pub.orcid.org/v3.0/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Country list with display names */
    private static final String[] COUNTRY_LIST = {
        "Afghanistan", "Albania", "Algeria", "Argentina", "Armenia",
        "Australia", "Austria", "Azerbaijan", "Bangladesh", "Belarus",
        "Belgium", "Bolivia", "Bosnia and Herzegovina", "Brazil", "Bulgaria",
        "Cambodia", "Cameroon", "Canada", "Chile", "China",
        "Colombia", "Costa Rica", "Croatia", "Cuba", "Cyprus",
        "Czech Republic", "Denmark", "Ecuador", "Egypt", "Estonia",
        "Ethiopia", "Finland", "France", "Georgia", "Germany",
        "Ghana", "Greece", "Guatemala", "Hong Kong", "Hungary",
        "Iceland", "India", "Indonesia", "Iran", "Iraq",
        "Ireland", "Israel", "Italy", "Japan", "Jordan",
        "Kazakhstan", "Kenya", "South Korea", "Kuwait", "Latvia",
        "Lebanon", "Lithuania", "Luxembourg", "Macau", "Malaysia",
        "Mexico", "Morocco", "Nepal", "Netherlands", "New Zealand",
        "Nigeria", "Norway", "Oman", "Pakistan", "Panama",
        "Peru", "Philippines", "Poland", "Portugal", "Qatar",
        "Romania", "Russia", "Saudi Arabia", "Serbia", "Singapore",
        "Slovakia", "Slovenia", "South Africa", "Spain", "Sri Lanka",
        "Sweden", "Switzerland", "Taiwan", "Tanzania", "Thailand",
        "Tunisia", "Turkey", "Uganda", "Ukraine", "United Arab Emirates",
        "United Kingdom", "United States", "Uruguay", "Uzbekistan",
        "Venezuela", "Vietnam", "Zimbabwe"
    };

    private TextField nameField;
    private TextField emailField;
    private TextArea affiliationField;
    private ComboBox<String> countryComboBox;
    private TextField orcidField;
    private Label orcidStatusLabel;
    private ProgressIndicator orcidSpinner;
    private Label validationLabel;

    // ORCID verification state
    private enum OrcidVerificationState { NONE, VERIFYING, VERIFIED, NOT_FOUND, ERROR }
    private volatile OrcidVerificationState orcidVerificationState = OrcidVerificationState.NONE;

    public LabHeadStep(SubmissionModel model) {
        super("lab-head",
              "Lab Head Information",
              "Provide contact details of the principal investigator / lab head",
              model);
    }

    @Override
    public boolean canSkip() {
        return model.isResubmissionMode();
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Info box
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(12));
        infoBox.setStyle(
            "-fx-background-color: #e7f3ff; " +
            "-fx-border-color: #0066cc; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;");

        Label infoTitle = new Label("Why do we need this?");
        infoTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc;");

        Label infoText = new Label(
            "We collect lab head information for grouping submissions by laboratory " +
            "and as a backup contact. This information will be included in the submission metadata.");
        infoText.setWrapText(true);
        infoText.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        infoBox.getChildren().addAll(infoTitle, infoText);

        // Form
        VBox formBox = new VBox(15);

        // Name
        VBox nameSection = createFieldSection("Name *", "Full name of the lab head / principal investigator", true);
        nameField = new TextField();
        nameField.setPromptText("e.g. John Arthur Smith");
        nameField.setPrefWidth(400);
        nameSection.getChildren().add(nameField);

        // Email
        VBox emailSection = createFieldSection("Email *", "Email address of the lab head", true);
        emailField = new TextField();
        emailField.setPromptText("e.g. john.smith@university.edu");
        emailField.setPrefWidth(400);
        emailSection.getChildren().add(emailField);

        // Affiliation
        VBox affiliationSection = createFieldSection("Affiliation *",
            "Department, laboratory, institute and country", true);
        affiliationField = new TextArea();
        affiliationField.setPromptText("e.g. Department of Biochemistry, University of Cambridge, Cambridge, UK");
        affiliationField.setPrefRowCount(3);
        affiliationField.setPrefWidth(400);
        affiliationField.setWrapText(true);
        affiliationSection.getChildren().add(affiliationField);

        // Country
        VBox countrySection = createFieldSection("Country *",
            "Country of the lab head's institution", true);
        countryComboBox = new ComboBox<>();
        countryComboBox.getItems().addAll(COUNTRY_LIST);
        countryComboBox.setPromptText("Select country...");
        countryComboBox.setPrefWidth(400);
        countryComboBox.setEditable(true);

        countrySection.getChildren().add(countryComboBox);

        // ORCID
        VBox orcidSection = createFieldSection("ORCID iD",
            "ORCID identifier of the lab head (recommended)", false);
        HBox orcidBox = new HBox(8);
        orcidBox.setAlignment(Pos.CENTER_LEFT);
        orcidField = new TextField();
        orcidField.setPromptText("e.g. 0000-0002-1825-0097");
        orcidField.setPrefWidth(250);

        orcidSpinner = new ProgressIndicator();
        orcidSpinner.setPrefSize(16, 16);
        orcidSpinner.setMaxSize(16, 16);
        orcidSpinner.setVisible(false);
        orcidSpinner.setManaged(false);

        orcidStatusLabel = new Label();
        orcidStatusLabel.setStyle("-fx-font-size: 11px;");
        orcidStatusLabel.setVisible(false);
        orcidStatusLabel.setManaged(false);

        Hyperlink orcidLink = new Hyperlink("What is ORCID?");
        orcidLink.setStyle("-fx-font-size: 11px;");
        orcidLink.setOnAction(e -> openUrl("https://orcid.org"));

        orcidBox.getChildren().addAll(orcidField, orcidSpinner, orcidStatusLabel, orcidLink);
        orcidSection.getChildren().add(orcidBox);

        // Validation status
        validationLabel = new Label();
        validationLabel.setStyle("-fx-font-weight: bold;");

        // Required note
        Label requiredNote = new Label("* Required fields");
        requiredNote.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");

        formBox.getChildren().addAll(
            nameSection,
            emailSection,
            affiliationSection,
            countrySection,
            new Separator(),
            orcidSection,
            new Separator(),
            validationLabel,
            requiredNote
        );

        content.getChildren().addAll(infoBox, formBox);
        scrollPane.setContent(content);
        return scrollPane;
    }

    private VBox createFieldSection(String title, String description, boolean required) {
        VBox section = new VBox(5);

        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        if (required) {
            Label reqLabel = new Label("REQUIRED");
            reqLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: #dc3545; " +
                "-fx-padding: 2 4; -fx-background-radius: 4;");
            titleBox.getChildren().addAll(titleLabel, reqLabel);
        } else {
            titleBox.getChildren().add(titleLabel);
        }

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        section.getChildren().addAll(titleBox, descLabel);
        return section;
    }

    @Override
    protected void initializeStep() {
        // Bind to model
        nameField.textProperty().bindBidirectional(model.labHeadNameProperty());
        emailField.textProperty().bindBidirectional(model.labHeadEmailProperty());
        affiliationField.textProperty().bindBidirectional(model.labHeadAffiliationProperty());
        orcidField.textProperty().bindBidirectional(model.labHeadOrcidProperty());

        // Bind country ComboBox to model
        countryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setLabHeadCountry(newVal);
        });
        // Pre-fill from model if available
        if (model.getLabHeadCountry() != null) {
            countryComboBox.setValue(model.getLabHeadCountry());
        }

        // Validation: name, email, affiliation, and country required
        valid.bind(Bindings.createBooleanBinding(() -> {
                String name = nameField.getText();
                String email = emailField.getText();
                String affiliation = affiliationField.getText();
                String country = countryComboBox.getValue();
                return name != null && !name.trim().isEmpty()
                    && email != null && EMAIL_PATTERN.matcher(email.trim()).matches()
                    && affiliation != null && !affiliation.trim().isEmpty()
                    && country != null && !country.trim().isEmpty();
            },
            nameField.textProperty(),
            emailField.textProperty(),
            affiliationField.textProperty(),
            countryComboBox.valueProperty()
        ));

        valid.addListener((obs, oldVal, newVal) -> updateValidationLabel());

        // Live email validation styling
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty() && !EMAIL_PATTERN.matcher(newVal.trim()).matches()) {
                emailField.setStyle("-fx-border-color: #dc3545;");
            } else {
                emailField.setStyle("");
            }
        });

        // Live ORCID validation styling + trigger verification when format is valid
        orcidField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                if (!ORCID_PATTERN.matcher(newVal.trim()).matches()) {
                    orcidField.setStyle("-fx-border-color: #dc3545;");
                    clearOrcidStatus();
                    orcidVerificationState = OrcidVerificationState.NONE;
                } else {
                    orcidField.setStyle("");
                    verifyOrcid(newVal.trim());
                }
            } else {
                orcidField.setStyle("");
                clearOrcidStatus();
                orcidVerificationState = OrcidVerificationState.NONE;
            }
        });
    }

    @Override
    protected void onStepEntering() {
        // Populate test mode examples
        if (model.isTrainingMode()) {
            if (nameField.getText() == null || nameField.getText().isEmpty()) {
                nameField.setText("John Smith");
                emailField.setText("john.smith@example.org");
                affiliationField.setText("Department of Proteomics, Test University, Test City, UK");
                countryComboBox.setValue("United Kingdom");
                orcidField.setText("0000-0002-1825-0097");
            }
        }
        updateValidationLabel();
    }

    @Override
    protected void onStepLeaving() {
        // Bidirectional binding keeps model in sync
    }

    private void updateValidationLabel() {
        boolean hasName = nameField.getText() != null && !nameField.getText().trim().isEmpty();
        boolean hasEmail = emailField.getText() != null && EMAIL_PATTERN.matcher(emailField.getText().trim()).matches();
        boolean hasAffiliation = affiliationField.getText() != null && !affiliationField.getText().trim().isEmpty();
        boolean hasCountry = countryComboBox.getValue() != null && !countryComboBox.getValue().trim().isEmpty();

        if (hasName && hasEmail && hasAffiliation && hasCountry) {
            validationLabel.setText("\u2714 All required fields completed");
            validationLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
        } else {
            StringBuilder missing = new StringBuilder("Missing: ");
            if (!hasName) missing.append("Name, ");
            if (!hasEmail) missing.append("Email, ");
            if (!hasAffiliation) missing.append("Affiliation, ");
            if (!hasCountry) missing.append("Country, ");
            String missingStr = missing.substring(0, missing.length() - 2);
            validationLabel.setText("\u26A0 " + missingStr);
            validationLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
        }
    }

    @Override
    public boolean validate() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showError("Please enter the lab head's name");
            nameField.requestFocus();
            return false;
        }

        String email = emailField.getText();
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            showError("Please enter a valid email address");
            emailField.requestFocus();
            return false;
        }

        if (affiliationField.getText() == null || affiliationField.getText().trim().isEmpty()) {
            showError("Please enter the lab head's affiliation");
            affiliationField.requestFocus();
            return false;
        }

        if (affiliationField.getText().length() > 500) {
            showError("Affiliation must be less than 500 characters");
            affiliationField.requestFocus();
            return false;
        }

        String country = countryComboBox.getValue();
        if (country == null || country.trim().isEmpty()) {
            showError("Please select the lab head's country");
            countryComboBox.requestFocus();
            return false;
        }

        // Validate ORCID format and existence if provided
        String orcid = orcidField.getText();
        if (orcid != null && !orcid.trim().isEmpty()) {
            if (!ORCID_PATTERN.matcher(orcid.trim()).matches()) {
                showError("ORCID iD must be in format: 0000-0000-0000-000X (four groups of four digits)");
                orcidField.requestFocus();
                return false;
            }
            if (orcidVerificationState == OrcidVerificationState.VERIFYING) {
                showError("ORCID verification is still in progress. Please wait a moment.");
                return false;
            }
            if (orcidVerificationState == OrcidVerificationState.NOT_FOUND) {
                showError("The ORCID iD entered was not found. Please check and correct it, or clear the field to skip.");
                orcidField.requestFocus();
                return false;
            }
            // Allow proceeding if verification had a network error (ERROR state) - don't block the user
        }

        return true;
    }

    private void verifyOrcid(String orcid) {
        orcidVerificationState = OrcidVerificationState.VERIFYING;
        showOrcidVerifying();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ORCID_API_URL + orcid))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Check if the field still has the same ORCID (user may have changed it)
                    Platform.runLater(() -> {
                        String currentText = orcidField.getText();
                        if (currentText == null || !currentText.trim().equals(orcid)) {
                            return; // User changed the field, ignore this response
                        }
                        if (response.statusCode() == 200) {
                            orcidVerificationState = OrcidVerificationState.VERIFIED;
                            showOrcidVerified();
                        } else if (response.statusCode() == 404) {
                            orcidVerificationState = OrcidVerificationState.NOT_FOUND;
                            showOrcidNotFound();
                        } else {
                            orcidVerificationState = OrcidVerificationState.ERROR;
                            showOrcidError("Could not verify (HTTP " + response.statusCode() + ")");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        String currentText = orcidField.getText();
                        if (currentText != null && currentText.trim().equals(orcid)) {
                            orcidVerificationState = OrcidVerificationState.ERROR;
                            showOrcidError("Could not verify (network error)");
                        }
                    });
                    return null;
                });
    }

    private void showOrcidVerifying() {
        orcidSpinner.setVisible(true);
        orcidSpinner.setManaged(true);
        orcidStatusLabel.setText("Verifying...");
        orcidStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        orcidStatusLabel.setVisible(true);
        orcidStatusLabel.setManaged(true);
    }

    private void showOrcidVerified() {
        orcidSpinner.setVisible(false);
        orcidSpinner.setManaged(false);
        orcidStatusLabel.setText("\u2714 Verified");
        orcidStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745; -fx-font-weight: bold;");
        orcidStatusLabel.setVisible(true);
        orcidStatusLabel.setManaged(true);
        orcidField.setStyle("-fx-border-color: #28a745;");
    }

    private void showOrcidNotFound() {
        orcidSpinner.setVisible(false);
        orcidSpinner.setManaged(false);
        orcidStatusLabel.setText("\u2718 ORCID not found");
        orcidStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc3545; -fx-font-weight: bold;");
        orcidStatusLabel.setVisible(true);
        orcidStatusLabel.setManaged(true);
        orcidField.setStyle("-fx-border-color: #dc3545;");
    }

    private void showOrcidError(String message) {
        orcidSpinner.setVisible(false);
        orcidSpinner.setManaged(false);
        orcidStatusLabel.setText("\u26A0 " + message);
        orcidStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffc107;");
        orcidStatusLabel.setVisible(true);
        orcidStatusLabel.setManaged(true);
        orcidField.setStyle("");
    }

    private void clearOrcidStatus() {
        orcidSpinner.setVisible(false);
        orcidSpinner.setManaged(false);
        orcidStatusLabel.setVisible(false);
        orcidStatusLabel.setManaged(false);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Missing Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.error("Failed to open URL: {}", url, e);
        }
    }
}
