package uk.ac.ebi.pride.pxsubmit.model;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central observable state for the submission wizard.
 * Replaces AppContext with JavaFX properties for automatic UI binding.
 *
 * Usage:
 * - Bind UI components directly to properties: textField.textProperty().bindBidirectional(model.projectTitleProperty())
 * - Listen to changes: model.filesProperty().addListener((obs, oldVal, newVal) -> ...)
 * - Tables bind directly: tableView.setItems(model.getFiles())
 */
public class SubmissionModel {

    // ==================== Submission Data ====================

    private final ObjectProperty<Submission> submission = new SimpleObjectProperty<>(new Submission());
    private final ObjectProperty<Resubmission> resubmission = new SimpleObjectProperty<>(new Resubmission());

    // Files - observable list for direct table binding
    private final ObservableList<DataFile> files = FXCollections.observableArrayList();
    private final ObservableSet<DataFile> uploadedFiles = FXCollections.observableSet();

    // Checksums - map from DataFile to checksum string
    private final ObservableMap<DataFile, String> checksums = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
    private final BooleanProperty checksumsCalculated = new SimpleBooleanProperty(false);

    // File ID counter
    private final IntegerProperty fileIdCounter = new SimpleIntegerProperty(0);

    // ==================== Credentials & Upload ====================

    private final StringProperty userName = new SimpleStringProperty();
    private final ObjectProperty<char[]> password = new SimpleObjectProperty<>();
    private final ObjectProperty<UploadDetail> uploadDetail = new SimpleObjectProperty<>();
    private final ObjectProperty<UploadMethod> uploadMethod = new SimpleObjectProperty<>();
    private final BooleanProperty summaryFileUploaded = new SimpleBooleanProperty(false);

    // ==================== Mode Flags ====================

    private final BooleanProperty resubmissionMode = new SimpleBooleanProperty(false);
    private final BooleanProperty trainingMode = new SimpleBooleanProperty(false);
    private final BooleanProperty controlledAccessMode = new SimpleBooleanProperty(false);
    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);
    private final BooleanProperty bulkMode = new SimpleBooleanProperty(false);

    // ==================== Submission Type ====================

    private final ObjectProperty<SubmissionTypeConstants> submissionType = new SimpleObjectProperty<>();

    // ==================== Project Metadata ====================

    private final StringProperty projectTitle = new SimpleStringProperty();
    private final StringProperty projectDescription = new SimpleStringProperty();
    private final StringProperty sampleProcessingProtocol = new SimpleStringProperty();
    private final StringProperty dataProcessingProtocol = new SimpleStringProperty();
    private final StringProperty keywords = new SimpleStringProperty();

    // Observable lists for metadata - bind directly to TableViews
    private final ObservableList<CvParam> species = FXCollections.observableArrayList();
    private final ObservableList<CvParam> tissues = FXCollections.observableArrayList();
    private final ObservableList<CvParam> cellTypes = FXCollections.observableArrayList();
    private final ObservableList<CvParam> diseases = FXCollections.observableArrayList();
    private final ObservableList<CvParam> instruments = FXCollections.observableArrayList();
    private final ObservableList<CvParam> modifications = FXCollections.observableArrayList();
    private final ObservableList<CvParam> quantifications = FXCollections.observableArrayList();
    private final ObservableList<CvParam> experimentMethods = FXCollections.observableArrayList();
    private final ObservableList<CvParam> software = FXCollections.observableArrayList();

    // ==================== Lab Head ====================

    private final StringProperty labHeadName = new SimpleStringProperty();
    private final StringProperty labHeadEmail = new SimpleStringProperty();
    private final StringProperty labHeadAffiliation = new SimpleStringProperty();

    // ==================== Computed Properties ====================

    private final ReadOnlyLongWrapper totalFileSize = new ReadOnlyLongWrapper();
    private final ReadOnlyIntegerWrapper totalFileCount = new ReadOnlyIntegerWrapper();

    public SubmissionModel() {
        // Bind computed properties
        totalFileCount.bind(Bindings.size(files));

        // Update total file size when files change
        files.addListener((javafx.collections.ListChangeListener.Change<? extends DataFile> c) -> {
            long total = files.stream()
                    .filter(f -> f.getFile() != null)
                    .mapToLong(f -> f.getFile().length())
                    .sum();
            totalFileSize.set(total);
        });
    }

    // ==================== File Operations ====================

    /**
     * Get the next file ID and increment the counter
     */
    public int nextFileId() {
        int id = fileIdCounter.get();
        fileIdCounter.set(id + 1);
        return id;
    }

    /**
     * Add a data file to the submission
     */
    public void addFile(DataFile dataFile) {
        if (!files.contains(dataFile)) {
            if (dataFile.getFileId() < 0) {
                dataFile.setFileId(fileIdCounter.get());
                fileIdCounter.set(fileIdCounter.get() + 1);
            }
            files.add(dataFile);

            // Always add to submission (UploadManager reads submission.getDataFiles())
            submission.get().addDataFile(dataFile);

            // Additionally track in resubmission for change-state management
            if (resubmissionMode.get()) {
                resubmission.get().addDataFile(dataFile);
                resubmission.get().getResubmission().put(dataFile, ResubmissionFileChangeState.ADD);
            }
        }
    }

    /**
     * Remove a data file from the submission
     */
    public void removeFile(DataFile dataFile) {
        // Don't allow removal of checksum.txt
        if ("checksum.txt".equals(dataFile.getFileName())) {
            return;
        }

        files.remove(dataFile);

        // Always remove from submission (keeps upload pipeline consistent)
        submission.get().removeDataFile(dataFile);

        // Additionally clean up resubmission tracking
        if (resubmissionMode.get()) {
            resubmission.get().removeDataFile(dataFile);
            resubmission.get().getResubmission().remove(dataFile);
        }
    }

    /**
     * Get files by type
     */
    public List<DataFile> getFilesByType(ProjectFileType type) {
        return files.stream()
                .filter(f -> f.getFileType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Set file type for a data file
     */
    public void setFileType(DataFile dataFile, ProjectFileType type) {
        if (dataFile != null && type != null && dataFile.getFileType() != type) {
            dataFile.setFileType(type);
            // Trigger list update for UI refresh
            int index = files.indexOf(dataFile);
            if (index >= 0) {
                files.set(index, dataFile);
            }
        }
    }

    /**
     * Mark a file as uploaded
     */
    public void markFileUploaded(DataFile dataFile) {
        uploadedFiles.add(dataFile);
    }

    /**
     * Check if file is uploaded
     */
    public boolean isFileUploaded(DataFile dataFile) {
        return uploadedFiles.contains(dataFile);
    }

    // ==================== Checksum Operations ====================

    /**
     * Store checksum for a file
     */
    public void setChecksum(DataFile dataFile, String checksum) {
        if (dataFile != null && checksum != null) {
            checksums.put(dataFile, checksum);
        }
    }

    /**
     * Store all checksums from a map
     */
    public void setChecksums(Map<DataFile, String> checksumMap) {
        checksums.clear();
        if (checksumMap != null) {
            checksums.putAll(checksumMap);
        }
        checksumsCalculated.set(!checksums.isEmpty());
    }

    /**
     * Get checksum for a file
     */
    public String getChecksum(DataFile dataFile) {
        return checksums.get(dataFile);
    }

    /**
     * Get all checksums
     */
    public ObservableMap<DataFile, String> getChecksums() {
        return checksums;
    }

    /**
     * Check if checksums have been calculated
     */
    public boolean areChecksumsCalculated() {
        return checksumsCalculated.get();
    }

    public BooleanProperty checksumsCalculatedProperty() {
        return checksumsCalculated;
    }

    /**
     * Clear all checksums
     */
    public void clearChecksums() {
        checksums.clear();
        checksumsCalculated.set(false);
    }

    // ==================== Metadata Operations ====================

    public void addSpecies(CvParam param) {
        if (!species.contains(param)) {
            species.add(param);
            submission.get().getProjectMetaData().addSpecies(param);
        }
    }

    public void removeSpecies(CvParam param) {
        species.remove(param);
        submission.get().getProjectMetaData().removeSpecies(param);
    }

    public void addTissue(CvParam param) {
        if (!tissues.contains(param)) {
            tissues.add(param);
            submission.get().getProjectMetaData().addTissues(param);
        }
    }

    public void removeTissue(CvParam param) {
        tissues.remove(param);
        submission.get().getProjectMetaData().removeTissues(param);
    }

    public void addCellType(CvParam param) {
        if (!cellTypes.contains(param)) {
            cellTypes.add(param);
            submission.get().getProjectMetaData().addCellTypes(param);
        }
    }

    public void removeCellType(CvParam param) {
        cellTypes.remove(param);
        submission.get().getProjectMetaData().removeCellTypes(param);
    }

    public void addDisease(CvParam param) {
        if (!diseases.contains(param)) {
            diseases.add(param);
            submission.get().getProjectMetaData().addDiseases(param);
        }
    }

    public void removeDisease(CvParam param) {
        diseases.remove(param);
        submission.get().getProjectMetaData().removeDiseases(param);
    }

    public void addInstrument(CvParam param) {
        if (!instruments.contains(param)) {
            instruments.add(param);
            submission.get().getProjectMetaData().addInstruments(param);
        }
    }

    public void removeInstrument(CvParam param) {
        instruments.remove(param);
        submission.get().getProjectMetaData().removeInstruments(param);
    }

    public void addModification(CvParam param) {
        if (!modifications.contains(param)) {
            modifications.add(param);
            submission.get().getProjectMetaData().addModifications(param);
        }
    }

    public void removeModification(CvParam param) {
        modifications.remove(param);
        submission.get().getProjectMetaData().removeModifications(param);
    }

    public void addQuantification(CvParam param) {
        if (!quantifications.contains(param)) {
            quantifications.add(param);
            submission.get().getProjectMetaData().addQuantifications(param);
        }
    }

    public void removeQuantification(CvParam param) {
        quantifications.remove(param);
        submission.get().getProjectMetaData().removeQuantifications(param);
    }

    public void addExperimentMethod(CvParam param) {
        if (!experimentMethods.contains(param)) {
            experimentMethods.add(param);
            submission.get().getProjectMetaData().addMassSpecExperimentMethods(param);
        }
    }

    public void removeExperimentMethod(CvParam param) {
        experimentMethods.remove(param);
        submission.get().getProjectMetaData().removeMassSpecExperimentMethods(param);
    }

    public void addSoftware(CvParam param) {
        if (!software.contains(param)) {
            software.add(param);
            submission.get().getProjectMetaData().addSoftwares(param);
        }
    }

    public void removeSoftware(CvParam param) {
        software.remove(param);
        submission.get().getProjectMetaData().removeSoftwares(param);
    }

    // ==================== State Management ====================

    /**
     * Reset the model for a new submission
     */
    public void reset() {
        // Zero the Contact submitter password before replacing Submission
        try {
            Contact submitter = submission.get().getProjectMetaData().getSubmitterContact();
            if (submitter != null) { submitter.setPassword(new char[0]); }
        } catch (Exception ignored) {}

        submission.set(new Submission());
        resubmission.set(new Resubmission());
        files.clear();
        uploadedFiles.clear();
        checksums.clear();
        checksumsCalculated.set(false);
        fileIdCounter.set(0);

        userName.set(null);
        clearPasswordArray();
        password.set(null);
        uploadDetail.set(null);
        summaryFileUploaded.set(false);

        species.clear();
        tissues.clear();
        cellTypes.clear();
        diseases.clear();
        instruments.clear();
        modifications.clear();
        quantifications.clear();
        experimentMethods.clear();
        software.clear();

        projectTitle.set(null);
        projectDescription.set(null);
        sampleProcessingProtocol.set(null);
        dataProcessingProtocol.set(null);
        keywords.set(null);

        labHeadName.set(null);
        labHeadEmail.set(null);
        labHeadAffiliation.set(null);

        submissionType.set(null);
        resubmissionMode.set(false);
        bulkMode.set(false);
    }

    /**
     * Sync project metadata from properties to submission object
     */
    public void syncMetadataToSubmission() {
        ProjectMetaData meta = submission.get().getProjectMetaData();
        if (meta == null) {
            meta = new ProjectMetaData();
            submission.get().setProjectMetaData(meta);
        }

        meta.setProjectTitle(projectTitle.get());
        meta.setProjectDescription(projectDescription.get());
        meta.setSampleProcessingProtocol(sampleProcessingProtocol.get());
        meta.setDataProcessingProtocol(dataProcessingProtocol.get());
        meta.setKeywords(keywords.get());
        meta.setSubmissionType(submissionType.get());

        // Lab head
        Contact labHead = new Contact();
        labHead.setName(labHeadName.get());
        labHead.setEmail(labHeadEmail.get());
        labHead.setAffiliation(labHeadAffiliation.get());
        meta.setLabHeadContact(labHead);
    }

    /**
     * Load metadata from submission object to properties
     */
    public void syncMetadataFromSubmission() {
        ProjectMetaData meta = submission.get().getProjectMetaData();
        if (meta != null) {
            projectTitle.set(meta.getProjectTitle());
            projectDescription.set(meta.getProjectDescription());
            sampleProcessingProtocol.set(meta.getSampleProcessingProtocol());
            dataProcessingProtocol.set(meta.getDataProcessingProtocol());
            keywords.set(meta.getKeywords());
            submissionType.set(meta.getSubmissionType());

            // Load lists
            species.setAll(meta.getSpecies());
            if (meta.getTissues() != null) tissues.setAll(meta.getTissues());
            if (meta.getCellTypes() != null) cellTypes.setAll(meta.getCellTypes());
            if (meta.getDiseases() != null) diseases.setAll(meta.getDiseases());
            instruments.setAll(meta.getInstruments());
            modifications.setAll(meta.getModifications());
            quantifications.setAll(meta.getQuantifications());
            experimentMethods.setAll(meta.getMassSpecExperimentMethods());

            // Lab head
            Contact labHead = meta.getLabHeadContact();
            if (labHead != null) {
                labHeadName.set(labHead.getName());
                labHeadEmail.set(labHead.getEmail());
                labHeadAffiliation.set(labHead.getAffiliation());
            }
        }

        // Load files
        files.setAll(submission.get().getDataFiles());
        fileIdCounter.set(files.stream().mapToInt(DataFile::getFileId).max().orElse(0) + 1);
    }

    // ==================== Property Accessors ====================

    // Submission
    public ObjectProperty<Submission> submissionProperty() { return submission; }
    public Submission getSubmission() { return submission.get(); }
    public void setSubmission(Submission value) { submission.set(value); syncMetadataFromSubmission(); }

    // Resubmission
    public ObjectProperty<Resubmission> resubmissionProperty() { return resubmission; }
    public Resubmission getResubmission() { return resubmission.get(); }
    public void setResubmission(Resubmission value) { resubmission.set(value); }

    // Files
    public ObservableList<DataFile> getFiles() { return files; }
    public ObservableSet<DataFile> getUploadedFiles() { return uploadedFiles; }

    // Credentials
    public StringProperty userNameProperty() { return userName; }
    public String getUserName() { return userName.get(); }
    public void setUserName(String value) { userName.set(value); }

    public ObjectProperty<char[]> passwordProperty() { return password; }
    public String getPassword() {
        char[] pw = password.get();
        return pw != null ? new String(pw) : null;
    }
    public void setPassword(String value) {
        clearPasswordArray();
        password.set(value != null ? value.toCharArray() : null);
    }
    public void setPassword(char[] value) {
        clearPasswordArray();
        password.set(value);
    }
    private void clearPasswordArray() {
        char[] old = password.get();
        if (old != null) { Arrays.fill(old, '\0'); }
    }

    // Upload
    public ObjectProperty<UploadDetail> uploadDetailProperty() { return uploadDetail; }
    public UploadDetail getUploadDetail() { return uploadDetail.get(); }
    public void setUploadDetail(UploadDetail value) { uploadDetail.set(value); }

    public ObjectProperty<UploadMethod> uploadMethodProperty() { return uploadMethod; }
    public UploadMethod getUploadMethod() { return uploadMethod.get(); }
    public void setUploadMethod(UploadMethod value) { uploadMethod.set(value); }

    public BooleanProperty summaryFileUploadedProperty() { return summaryFileUploaded; }
    public boolean isSummaryFileUploaded() { return summaryFileUploaded.get(); }
    public void setSummaryFileUploaded(boolean value) { summaryFileUploaded.set(value); }

    // Mode flags
    public BooleanProperty resubmissionModeProperty() { return resubmissionMode; }
    public boolean isResubmissionMode() { return resubmissionMode.get(); }
    public void setResubmissionMode(boolean value) { resubmissionMode.set(value); }

    public BooleanProperty trainingModeProperty() { return trainingMode; }
    public boolean isTrainingMode() { return trainingMode.get(); }
    public void setTrainingMode(boolean value) { trainingMode.set(value); }

    public BooleanProperty controlledAccessModeProperty() { return controlledAccessMode; }
    public boolean isControlledAccessMode() { return controlledAccessMode.get(); }
    public void setControlledAccessMode(boolean value) { controlledAccessMode.set(value); }

    public BooleanProperty loggedInProperty() { return loggedIn; }
    public boolean isLoggedIn() { return loggedIn.get(); }
    public void setLoggedIn(boolean value) { loggedIn.set(value); }

    public BooleanProperty bulkModeProperty() { return bulkMode; }
    public boolean isBulkMode() { return bulkMode.get(); }
    public void setBulkMode(boolean value) { bulkMode.set(value); }

    // Submission type
    public ObjectProperty<SubmissionTypeConstants> submissionTypeProperty() { return submissionType; }
    public SubmissionTypeConstants getSubmissionType() { return submissionType.get(); }
    public void setSubmissionType(SubmissionTypeConstants value) { submissionType.set(value); }

    // Project metadata
    public StringProperty projectTitleProperty() { return projectTitle; }
    public String getProjectTitle() { return projectTitle.get(); }
    public void setProjectTitle(String value) { projectTitle.set(value); }

    public StringProperty projectDescriptionProperty() { return projectDescription; }
    public String getProjectDescription() { return projectDescription.get(); }
    public void setProjectDescription(String value) { projectDescription.set(value); }

    public StringProperty sampleProcessingProtocolProperty() { return sampleProcessingProtocol; }
    public String getSampleProcessingProtocol() { return sampleProcessingProtocol.get(); }
    public void setSampleProcessingProtocol(String value) { sampleProcessingProtocol.set(value); }

    public StringProperty dataProcessingProtocolProperty() { return dataProcessingProtocol; }
    public String getDataProcessingProtocol() { return dataProcessingProtocol.get(); }
    public void setDataProcessingProtocol(String value) { dataProcessingProtocol.set(value); }

    public StringProperty keywordsProperty() { return keywords; }
    public String getKeywords() { return keywords.get(); }
    public void setKeywords(String value) { keywords.set(value); }

    // Metadata lists
    public ObservableList<CvParam> getSpecies() { return species; }
    public ObservableList<CvParam> getTissues() { return tissues; }
    public ObservableList<CvParam> getCellTypes() { return cellTypes; }
    public ObservableList<CvParam> getDiseases() { return diseases; }
    public ObservableList<CvParam> getInstruments() { return instruments; }
    public ObservableList<CvParam> getModifications() { return modifications; }
    public ObservableList<CvParam> getQuantifications() { return quantifications; }
    public ObservableList<CvParam> getExperimentMethods() { return experimentMethods; }
    public ObservableList<CvParam> getSoftware() { return software; }

    // Lab head
    public StringProperty labHeadNameProperty() { return labHeadName; }
    public String getLabHeadName() { return labHeadName.get(); }
    public void setLabHeadName(String value) { labHeadName.set(value); }

    public StringProperty labHeadEmailProperty() { return labHeadEmail; }
    public String getLabHeadEmail() { return labHeadEmail.get(); }
    public void setLabHeadEmail(String value) { labHeadEmail.set(value); }

    public StringProperty labHeadAffiliationProperty() { return labHeadAffiliation; }
    public String getLabHeadAffiliation() { return labHeadAffiliation.get(); }
    public void setLabHeadAffiliation(String value) { labHeadAffiliation.set(value); }

    // Computed read-only properties
    public ReadOnlyLongProperty totalFileSizeProperty() { return totalFileSize.getReadOnlyProperty(); }
    public long getTotalFileSize() { return totalFileSize.get(); }

    public ReadOnlyIntegerProperty totalFileCountProperty() { return totalFileCount.getReadOnlyProperty(); }
    public int getTotalFileCount() { return totalFileCount.get(); }
}
