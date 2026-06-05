package uk.ac.ebi.pride.pxsubmit.model;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks SDRF validator results for the current submission file set.
 * Clears results when SDRF files are added, removed, or modified.
 */
public class SdrfValidationTracker {

    private String fileSetSignature;
    private String validatedSignature;
    private final Map<String, Boolean> validationByPath = new HashMap<>();

    public static String buildSignature(List<File> sdrfFiles) {
        List<String> ids = sdrfFiles.stream()
                .map(f -> f.getAbsolutePath() + ":" + f.lastModified() + ":" + f.length())
                .sorted()
                .toList();
        return String.join("|", ids);
    }

    public void syncFileSet(List<File> sdrfFiles) {
        String newSignature = sdrfFiles.isEmpty() ? null : buildSignature(sdrfFiles);
        if (Objects.equals(newSignature, fileSetSignature)) {
            return;
        }
        fileSetSignature = newSignature;
        validatedSignature = null;
        validationByPath.clear();
    }

    public void clear() {
        fileSetSignature = null;
        validatedSignature = null;
        validationByPath.clear();
    }

    public boolean isValidatedForCurrentFiles(List<File> sdrfFiles) {
        if (sdrfFiles.isEmpty()) {
            return true;
        }
        String signature = buildSignature(sdrfFiles);
        return signature.equals(validatedSignature) && allMarkedPassed(sdrfFiles);
    }

    public boolean allMarkedPassed(List<File> sdrfFiles) {
        if (sdrfFiles.isEmpty()) {
            return true;
        }
        for (File file : sdrfFiles) {
            if (!Boolean.TRUE.equals(validationByPath.get(file.getAbsolutePath()))) {
                return false;
            }
        }
        return true;
    }

    public boolean hasAnyFailed(List<File> sdrfFiles) {
        for (File file : sdrfFiles) {
            if (Boolean.FALSE.equals(validationByPath.get(file.getAbsolutePath()))) {
                return true;
            }
        }
        return false;
    }

    public void applyResults(String signature, Map<String, Boolean> resultsByPath, boolean allPassed) {
        fileSetSignature = signature;
        validationByPath.clear();
        if (resultsByPath != null) {
            validationByPath.putAll(resultsByPath);
        }
        if (allPassed) {
            validatedSignature = signature;
        } else {
            validatedSignature = null;
        }
    }

    public Boolean lookup(String absolutePath) {
        return validationByPath.get(absolutePath);
    }

    public Map<String, Boolean> getValidationByPath() {
        return Map.copyOf(validationByPath);
    }
}
