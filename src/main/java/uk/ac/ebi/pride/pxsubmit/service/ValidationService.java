package uk.ac.ebi.pride.pxsubmit.service;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.data.validation.ValidationMessage;
import uk.ac.ebi.pride.data.validation.ValidationReport;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating submission files.
 * Performs file type detection, format validation, and submission rule checking.
 *
 * Validation includes:
 * - File existence and readability
 * - File format detection (PRIDE XML, mzIdentML, mzTab, etc.)
 * - Submission type rules (COMPLETE, PARTIAL, RAW)
 * - Result file validation
 * - Peak list file references for mzIdentML
 *
 * Usage:
 * <pre>
 * ValidationService service = new ValidationService(files, submissionType);
 *
 * progressBar.progressProperty().bind(service.progressProperty());
 * statusLabel.textProperty().bind(service.messageProperty());
 *
 * service.setOnSucceeded(e -> {
 *     ValidationResult result = service.getValue();
 *     if (result.isValid()) {
 *         // Proceed
 *     } else {
 *         // Show errors
 *     }
 * });
 *
 * service.start();
 * </pre>
 */
public class ValidationService extends Service<ValidationService.ValidationResult> {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    // mzIdentML version patterns
    private static final String MZIDENTML_VERSION_1_1 = "1.1.0";
    private static final String MZIDENTML_VERSION_1_2 = "1.2.0";

    /**
     * Shared StAX factory hardened against XXE (no DTDs, no external entities).
     * mzIdentML is parsed as a stream so multi-line/pretty-printed elements are
     * handled correctly and large files are never loaded into memory.
     */
    private static final XMLInputFactory XML_INPUT_FACTORY = createHardenedXmlInputFactory();

    private static XMLInputFactory createHardenedXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
        return factory;
    }

    private final ObservableList<DataFile> files;
    private final SubmissionTypeConstants submissionType;

    // Detailed progress
    private final StringProperty currentFile = new SimpleStringProperty();
    private final IntegerProperty filesValidated = new SimpleIntegerProperty(0);

    public ValidationService(ObservableList<DataFile> files, SubmissionTypeConstants submissionType) {
        this.files = files;
        this.submissionType = submissionType;
    }

    @Override
    protected Task<ValidationResult> createTask() {
        return new ValidationTask(new ArrayList<>(files), submissionType);
    }

    /**
     * The validation task
     */
    private class ValidationTask extends Task<ValidationResult> {

        private final List<DataFile> taskFiles;
        private final SubmissionTypeConstants taskSubmissionType;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public ValidationTask(List<DataFile> files, SubmissionTypeConstants submissionType) {
            this.taskFiles = files;
            this.taskSubmissionType = submissionType;
        }

        @Override
        protected ValidationResult call() throws Exception {
            logger.info("Starting validation for {} files, submission type: {}",
                taskFiles.size(), taskSubmissionType);

            int total = taskFiles.size();
            int validated = 0;

            // Phase 1: Quick validation (file existence, readability)
            updateMessage("Checking file accessibility...");
            ValidationReport quickReport = runQuickValidation();
            if (quickReport.hasError()) {
                for (ValidationMessage msg : quickReport.getMessages()) {
                    if (msg.getType() == ValidationMessage.Type.ERROR) {
                        errors.add(msg.getMessage());
                    }
                }
                logger.error("Quick validation failed with {} errors", errors.size());
                return new ValidationResult(false, errors, warnings, taskFiles);
            }
            updateProgress(20, 100);

            // Phase 2: File type detection and categorization
            updateMessage("Detecting file types...");
            Map<ProjectFileType, List<DataFile>> filesByType = categorizeFiles();
            updateProgress(40, 100);

            // Phase 3: Submission type validation
            updateMessage("Validating submission rules...");
            if (!validateSubmissionType(filesByType)) {
                return new ValidationResult(false, errors, warnings, taskFiles);
            }
            updateProgress(60, 100);

            // Phase 4: Format-specific validation
            updateMessage("Validating file formats...");
            if (!validateFileFormats(filesByType)) {
                return new ValidationResult(false, errors, warnings, taskFiles);
            }
            updateProgress(80, 100);

            // Phase 5: Cross-file validation (references, mappings)
            updateMessage("Validating file relationships...");
            validateFileRelationships(filesByType);
            updateProgress(100, 100);

            boolean isValid = errors.isEmpty();
            logger.info("Validation completed. Valid: {}, Errors: {}, Warnings: {}",
                isValid, errors.size(), warnings.size());

            return new ValidationResult(isValid, errors, warnings, taskFiles);
        }

        /**
         * Quick validation - file existence and readability
         */
        private ValidationReport runQuickValidation() {
            ValidationReport report = new ValidationReport();

            for (DataFile dataFile : taskFiles) {
                File file = dataFile.getFile();
                String fileName = dataFile.getFileName();

                Platform.runLater(() -> currentFile.set(fileName));

                if (file == null) {
                    report.addMessage(new ValidationMessage(ValidationMessage.Type.ERROR,
                        "File is null: " + fileName));
                    continue;
                }

                if (!file.exists()) {
                    report.addMessage(new ValidationMessage(ValidationMessage.Type.ERROR,
                        "File does not exist: " + fileName));
                    continue;
                }

                if (!file.canRead()) {
                    report.addMessage(new ValidationMessage(ValidationMessage.Type.ERROR,
                        "File is not readable: " + fileName));
                    continue;
                }

                if (file.length() == 0) {
                    report.addMessage(new ValidationMessage(ValidationMessage.Type.WARNING,
                        "File is empty: " + fileName));
                }

                // Use SubmissionValidator for detailed validation
                ValidationReport fileReport = SubmissionValidator.validateDataFile(dataFile);
                if (fileReport != null) {
                    for (ValidationMessage msg : fileReport.getMessages()) {
                        report.addMessage(msg);
                    }
                }
            }

            return report;
        }

        /**
         * Categorize files by type
         */
        private Map<ProjectFileType, List<DataFile>> categorizeFiles() {
            Map<ProjectFileType, List<DataFile>> result = new EnumMap<>(ProjectFileType.class);

            for (DataFile dataFile : taskFiles) {
                ProjectFileType type = dataFile.getFileType();
                result.computeIfAbsent(type, k -> new ArrayList<>()).add(dataFile);
            }

            return result;
        }

        /**
         * Validate submission type rules
         */
        private boolean validateSubmissionType(Map<ProjectFileType, List<DataFile>> filesByType) {
            List<DataFile> resultFiles = filesByType.getOrDefault(ProjectFileType.RESULT, Collections.emptyList());
            List<DataFile> rawFiles = filesByType.getOrDefault(ProjectFileType.RAW, Collections.emptyList());
            List<DataFile> searchFiles = filesByType.getOrDefault(ProjectFileType.SEARCH, Collections.emptyList());

            boolean hasResult = !resultFiles.isEmpty();
            boolean hasRaw = !rawFiles.isEmpty();
            boolean hasSearch = !searchFiles.isEmpty();

            switch (taskSubmissionType) {
                case COMPLETE:
                    if (!hasResult) {
                        errors.add("Complete submission requires result files (PRIDE XML, mzIdentML, or mzTab)");
                        return false;
                    }
                    if (!hasRaw) {
                        errors.add("Complete submission requires raw files");
                        return false;
                    }
                    break;

                case PARTIAL:
                    if (!hasSearch) {
                        errors.add("Partial submission requires search engine output files");
                        return false;
                    }
                    if (!hasRaw) {
                        errors.add("Partial submission requires raw files");
                        return false;
                    }
                    if (hasResult) {
                        errors.add("Partial submission should not contain result files");
                        return false;
                    }
                    break;

                case RAW:
                    if (!hasRaw) {
                        errors.add("RAW submission requires raw files");
                        return false;
                    }
                    if (hasResult || hasSearch) {
                        errors.add("RAW submission should only contain raw files");
                        return false;
                    }
                    break;

                case AFFINITY:
                    // Affinity submission has different rules
                    if (!hasRaw) {
                        warnings.add("Affinity submission typically includes raw data files");
                    }
                    break;
            }

            return true;
        }

        /**
         * Validate format-specific rules
         */
        private boolean validateFileFormats(Map<ProjectFileType, List<DataFile>> filesByType) {
            List<DataFile> resultFiles = filesByType.getOrDefault(ProjectFileType.RESULT, Collections.emptyList());

            // Get files by format
            List<DataFile> prideXmlFiles = getFilesByFormat(resultFiles, MassSpecFileFormat.PRIDE);
            List<DataFile> mzIdentMLFiles = getFilesByFormat(resultFiles, MassSpecFileFormat.MZIDENTML);
            List<DataFile> mzTabFiles = getFilesByFormat(resultFiles, MassSpecFileFormat.MZTAB);

            // Cannot mix result file formats
            int formatCount = 0;
            if (!prideXmlFiles.isEmpty()) formatCount++;
            if (!mzIdentMLFiles.isEmpty()) formatCount++;
            if (!mzTabFiles.isEmpty()) formatCount++;

            if (formatCount > 1) {
                errors.add("Cannot mix different result file formats (PRIDE XML, mzIdentML, mzTab)");
                return false;
            }

            // Validate mzIdentML version
            for (DataFile file : mzIdentMLFiles) {
                if (!validateMzIdentMLVersion(file)) {
                    errors.add("Invalid mzIdentML version in file: " + file.getFileName() +
                        ". Only versions 1.1.0 and 1.2.0 are supported.");
                    return false;
                }
            }

            return true;
        }

        /**
         * Validate file relationships
         */
        private void validateFileRelationships(Map<ProjectFileType, List<DataFile>> filesByType) {
            List<DataFile> resultFiles = filesByType.getOrDefault(ProjectFileType.RESULT, Collections.emptyList());
            List<DataFile> mzIdentMLFiles = getFilesByFormat(resultFiles, MassSpecFileFormat.MZIDENTML);

            // Check mzIdentML peak list references
            for (DataFile mzIdentML : mzIdentMLFiles) {
                Set<String> referencedFiles = extractMzIdentMLPeakListReferences(mzIdentML);
                for (String ref : referencedFiles) {
                    if (!hasFileWithName(ref)) {
                        warnings.add("mzIdentML file '" + mzIdentML.getFileName() +
                            "' references peak list file '" + ref + "' which is not in the submission");
                    }
                }
            }

            // Check WIFF files have corresponding .scan files
            for (DataFile dataFile : taskFiles) {
                String ext = FilenameUtils.getExtension(dataFile.getFileName()).toLowerCase();
                if ("wiff".equals(ext)) {
                    String scanFileName = dataFile.getFileName() + ".scan";
                    if (!hasFileWithName(scanFileName)) {
                        warnings.add("WIFF file '" + dataFile.getFileName() +
                            "' should have a corresponding .scan file");
                    }
                }
            }
        }

        private List<DataFile> getFilesByFormat(List<DataFile> files, MassSpecFileFormat format) {
            return files.stream()
                .filter(f -> {
                    try {
                        MassSpecFileFormat fileFormat = MassSpecFileFormat.checkFormat(f.getFile());
                        return fileFormat == format;
                    } catch (java.io.IOException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        }

        private boolean validateMzIdentMLVersion(DataFile file) {
            XMLStreamReader xml = null;
            try (InputStream in = new BufferedInputStream(new FileInputStream(file.getFile()))) {
                xml = XML_INPUT_FACTORY.createXMLStreamReader(in);
                while (xml.hasNext()) {
                    if (xml.next() == XMLStreamConstants.START_ELEMENT
                            && "MzIdentML".equals(xml.getLocalName())) {
                        String version = xml.getAttributeValue(null, "version");
                        if (version == null) {
                            // Root element has no version attribute; nothing to validate against.
                            return true;
                        }
                        return MZIDENTML_VERSION_1_1.equals(version)
                                || MZIDENTML_VERSION_1_2.equals(version);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not validate mzIdentML version for: {}", file.getFileName(), e);
            } finally {
                closeQuietly(xml);
            }
            return true; // Assume valid if we cannot detect a version
        }

        private Set<String> extractMzIdentMLPeakListReferences(DataFile file) {
            Set<String> references = new HashSet<>();
            XMLStreamReader xml = null;
            try (InputStream in = new BufferedInputStream(new FileInputStream(file.getFile()))) {
                xml = XML_INPUT_FACTORY.createXMLStreamReader(in);
                while (xml.hasNext()) {
                    int event = xml.next();
                    if (event == XMLStreamConstants.START_ELEMENT
                            && "SpectraData".equals(xml.getLocalName())) {
                        String location = xml.getAttributeValue(null, "location");
                        if (location != null && !location.isEmpty()) {
                            // Extract filename from path/URL (handle both separators)
                            String fileName = location.substring(
                                Math.max(location.lastIndexOf('/'), location.lastIndexOf('\\')) + 1);
                            references.add(fileName);
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT
                            && "Inputs".equals(xml.getLocalName())) {
                        break; // SpectraData is declared within the Inputs section
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not extract peak list references from: {}", file.getFileName(), e);
            } finally {
                closeQuietly(xml);
            }
            return references;
        }

        private void closeQuietly(XMLStreamReader xml) {
            if (xml != null) {
                try {
                    xml.close();
                } catch (Exception ignored) {
                    // best-effort close
                }
            }
        }

        private boolean hasFileWithName(String fileName) {
            return taskFiles.stream()
                .anyMatch(f -> f.getFileName().equalsIgnoreCase(fileName));
        }
    }

    // ==================== Property Accessors ====================

    public ReadOnlyStringProperty currentFileProperty() {
        return currentFile;
    }

    public ReadOnlyIntegerProperty filesValidatedProperty() {
        return filesValidated;
    }

    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final List<DataFile> files;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings, List<DataFile> files) {
            this.valid = valid;
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
            this.files = Collections.unmodifiableList(new ArrayList<>(files));
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<DataFile> getFiles() {
            return files;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getErrorSummary() {
            if (errors.isEmpty()) {
                return "No errors";
            }
            return String.join("\n", errors);
        }

        public String getWarningSummary() {
            if (warnings.isEmpty()) {
                return "No warnings";
            }
            return String.join("\n", warnings);
        }
    }
}
