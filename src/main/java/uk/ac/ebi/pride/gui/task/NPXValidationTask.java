package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NPXValidationTask {

    private static final Logger logger = LoggerFactory.getLogger(NPXValidationTask.class);

    private static Set<String> mandatoryColumns = new HashSet<>();

    static {
        // Add the names of mandatory columns here
        mandatoryColumns.add("SampleID");
        mandatoryColumns.add("OlinkID");
        mandatoryColumns.add("UniProt");
        mandatoryColumns.add("Assay");
        mandatoryColumns.add("MissingFreq");
        mandatoryColumns.add("Panel");
        mandatoryColumns.add("LOD");
        mandatoryColumns.add("Normalization");
        mandatoryColumns.add("PlateID");
        mandatoryColumns.add("NPX");
    }

    public static boolean validate(File file) {
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            logger.error("The file does not exist, is not a valid file, or cannot be read: {}", file.getAbsolutePath());
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Read the first line to get column headers
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                logger.error("The file is empty or does not have a valid header row: {}", file.getAbsolutePath());
                return false;
            }

            // Detect delimiter (comma or semicolon)
            String delimiter = headerLine.contains(";") ? ";" : ",";

            // Split the header line into column names
            String[] columns = headerLine.split(delimiter);

            // Remove double quotes from column names
            Set<String> columnSet = new HashSet<>();
            for (String column : columns) {
                columnSet.add(column.replaceAll("^\"|\"$", "").trim()); // Remove surrounding quotes
            }

            // Check for missing mandatory columns
            Set<String> missingColumns = new HashSet<>(mandatoryColumns);
            missingColumns.removeAll(columnSet);

            if (!missingColumns.isEmpty()) {
                logger.error("The file is missing mandatory columns: {}", missingColumns);
                return false;
            }

            logger.info("The file {} passed validation successfully.", file.getName());
            return true;

        } catch (IOException e) {
            logger.error("An error occurred while reading the file: {}", file.getAbsolutePath(), e);
            return false;
        }
    }
}