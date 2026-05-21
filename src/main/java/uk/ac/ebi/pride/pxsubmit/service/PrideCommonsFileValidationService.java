package uk.ac.ebi.pride.pxsubmit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.submissions.commons.dtos.ValidationRequestDTO;
import uk.ac.ebi.pride.submissions.commons.dtos.ValidationResponseDTO;
import uk.ac.ebi.pride.submissions.commons.exceptions.SubValidationException;
import uk.ac.ebi.pride.submissions.commons.services.SubmissionQueuedRequestsService;
import uk.ac.ebi.pride.submissions.commons.services.SubmissionValidationService;
import uk.ac.ebi.pride.submissions.commons.services.impl.SubmissionValidationServiceImpl;
import uk.ac.ebi.pride.submissions.commons.types.SeverityLevel;
import uk.ac.ebi.pride.submissions.commons.types.ValidateRunState;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Validates submission files using {@code pride-submissions-commons} 1.0.5+
 * ({@link SubmissionValidationService} + PRIDE YAML rules).
 */
public class PrideCommonsFileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PrideCommonsFileValidationService.class);

    private final SubmissionValidationServiceImpl validationService = new SubmissionValidationServiceImpl();
    private volatile boolean configLoaded;

    /**
     * Loads validation rules from the remote PRIDE configuration (cached by commons).
     */
    public synchronized void preloadConfig() throws SubValidationException {
        if (configLoaded) {
            return;
        }
        logger.info("Loading PRIDE submission validation rules from {}", validationService.getRemoteUrl());
        validationService.loadRemoteValidationConfig();
        configLoaded = true;
    }

    public ValidationResult validate(List<DataFile> files) throws SubValidationException {
        if (files == null || files.isEmpty()) {
            return ValidationResult.failure(List.of("No files selected for validation."));
        }

        preloadConfig();

        if (validationService.getCurrentValidationConfigData().isEmpty()) {
            throw new SubValidationException("PRIDE validation configuration is not available.", 0);
        }

        String ticketId = UUID.randomUUID().toString();
        ValidationRequestDTO request = new ValidationRequestDTO();
        request.setTicketId(ticketId);
        request.setPerformFileReads(false);

        SubmissionQueuedRequestsService queueService = new ModelFilesQueuedRequestsService(files);

        try {
            validationService.queueValidation(request, queueService, null);
            validationService.runValidationJob(ticketId, queueService, files.size());
            ValidationResponseDTO response = validationService.checkValidation(queueService, ticketId);
            return ValidationResult.from(response);
        } catch (JsonProcessingException e) {
            throw new SubValidationException(e);
        }
    }

    public record ValidationResult(boolean valid, List<String> errors, List<String> warnings, String summaryMessage) {

        public static ValidationResult from(ValidationResponseDTO response) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            if (response.getMessage() != null && !response.getMessage().isBlank()) {
                if (response.getSeverity() == SeverityLevel.ERROR) {
                    errors.add(response.getMessage());
                } else if (response.getSeverity() == SeverityLevel.WARNING) {
                    warnings.add(response.getMessage());
                }
            }

            List<ValidationResponseDTO.FileValidateProgress> processed = response.getProcessedFiles();
            if (processed != null) {
                for (ValidationResponseDTO.FileValidateProgress entry : processed) {
                    if (!entry.isValid()) {
                        errors.add(describeProgress(entry));
                    }
                }
            }

            boolean finished = response.getState() == ValidateRunState.FINISHED || response.isFinished();
            boolean valid = errors.isEmpty()
                    && response.getSeverity() != SeverityLevel.ERROR
                    && finished;

            if (!finished && response.getState() == ValidateRunState.QUEUED) {
                errors.add("Validation did not complete.");
                valid = false;
            }

            String summary = response.getMessage();
            if (summary == null || summary.isBlank()) {
                summary = valid ? "PRIDE file validation passed." : "PRIDE file validation failed.";
            }
            return new ValidationResult(valid, List.copyOf(errors), List.copyOf(warnings), summary);
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, List.copyOf(errors), List.of(),
                    errors.isEmpty() ? "Validation failed." : errors.get(0));
        }

        public String formattedDetails() {
            StringBuilder text = new StringBuilder();
            if (summaryMessage != null && !summaryMessage.isBlank()) {
                text.append(summaryMessage);
            }
            if (!errors.isEmpty()) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append("Errors:\n");
                for (String error : errors) {
                    text.append("• ").append(error).append('\n');
                }
            }
            if (!warnings.isEmpty()) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append("Warnings:\n");
                for (String warning : warnings) {
                    text.append("• ").append(warning).append('\n');
                }
            }
            return text.toString().trim();
        }

        private static String describeProgress(ValidationResponseDTO.FileValidateProgress entry) {
            String name = entry.getName();
            if (name == null || name.isBlank()) {
                name = entry.getFileEntry() != null ? entry.getFileEntry().getName() : "file";
            }
            String message = entry.getMessage();
            if (message != null && !message.isBlank()) {
                return name + ": " + message;
            }
            return "Invalid file: " + name;
        }
    }
}
