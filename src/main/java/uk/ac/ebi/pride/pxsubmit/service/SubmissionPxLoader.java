package uk.ac.ebi.pride.pxsubmit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads an existing PX summary file for a selected submission ticket.
 */
public class SubmissionPxLoader {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionPxLoader.class);

    private final String configuredPath;

    public SubmissionPxLoader() {
        this(AppConfig.getInstance().getLocalSubmissionFilePath());
    }

    SubmissionPxLoader(String configuredPath) {
        this.configuredPath = configuredPath;
    }

    public File resolveSubmissionFile(String ticketId) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }

        String resolvedPath = configuredPath.replace("{ticketId}", ticketId != null ? ticketId : "");
        return new File(expandUserHome(resolvedPath));
    }

    public Submission loadForTicket(String ticketId) throws IOException {
        return load(resolveSubmissionFile(ticketId));
    }

    public Submission load(File submissionFile) throws IOException {
        if (submissionFile == null) {
            throw new FileNotFoundException("No submission.px path is configured");
        }

        if (!submissionFile.isFile()) {
            throw new FileNotFoundException("submission.px was not found at " + submissionFile.getAbsolutePath());
        }

        try {
            logger.info("Loading submission metadata from {}", submissionFile.getAbsolutePath());
            Submission submission = SubmissionFileParser.parse(submissionFile);
            restoreCustomLabHeadComments(submission, submissionFile);
            return submission;
        } catch (Exception e) {
            throw new IOException("Could not parse " + submissionFile.getAbsolutePath(), e);
        }
    }

    private void restoreCustomLabHeadComments(Submission submission, File submissionFile) throws IOException {
        List<String> comments = new ArrayList<>();
        if (submission.getComments() != null) {
            submission.getComments().stream()
                .filter(comment -> !isRegeneratedToolComment(comment))
                .filter(comment -> !isCustomLabHeadComment(comment))
                .forEach(comments::add);
        }

        for (String line : Files.readAllLines(submissionFile.toPath(), StandardCharsets.UTF_8)) {
            String normalized = normalizeCustomLabHeadComment(line);
            if (normalized != null && !comments.contains(normalized)) {
                comments.add(normalized);
            }
        }

        submission.setComments(comments);
    }

    private String normalizeCustomLabHeadComment(String line) {
        if (line == null || !line.startsWith("COM\t")) {
            return null;
        }

        String[] parts = line.split("\t", 3);
        if (parts.length < 3) {
            return null;
        }

        String key = parts[1] != null ? parts[1].trim() : "";
        String value = parts[2] != null ? parts[2].trim() : "";
        if (value.isEmpty()) {
            return null;
        }

        if ("lab_head_country".equals(key) || "lab_head_orcid".equals(key)) {
            return key + "\t" + value;
        }

        return null;
    }

    private boolean isRegeneratedToolComment(String comment) {
        if (comment == null) {
            return false;
        }

        String trimmed = comment.trim();
        return trimmed.startsWith("Version:")
            || trimmed.startsWith("Operating System:");
    }

    private boolean isCustomLabHeadComment(String comment) {
        if (comment == null) {
            return false;
        }

        String trimmed = comment.trim();
        return trimmed.equals("lab_head_country")
            || trimmed.startsWith("lab_head_country\t")
            || trimmed.equals("lab_head_orcid")
            || trimmed.startsWith("lab_head_orcid\t");
    }

    private String expandUserHome(String path) {
        if (path.equals("~")) {
            return System.getProperty("user.home");
        }

        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }

        return path;
    }
}
