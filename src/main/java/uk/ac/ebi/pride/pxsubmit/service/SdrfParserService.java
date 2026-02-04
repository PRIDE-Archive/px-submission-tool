package uk.ac.ebi.pride.pxsubmit.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.CvParam;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing SDRF (Sample and Data Relationship Format) files.
 * Extracts sample metadata including organism, tissue, cell type, disease,
 * instrument, and modifications.
 *
 * SDRF Format:
 * - Tab-delimited TSV file
 * - First row contains column headers
 * - Headers follow pattern: characteristics[term] or comment[term]
 * - Values may include ontology annotations: term name (ONTOLOGY:accession)
 *
 * Supported columns:
 * - characteristics[organism]: Species
 * - characteristics[organism part]: Tissue
 * - characteristics[cell type]: Cell type
 * - characteristics[disease]: Disease
 * - comment[instrument]: Mass spectrometer
 * - comment[modification parameters]: PTMs
 * - comment[label]: Labeling method
 *
 * Usage:
 * <pre>
 * SdrfParserService parser = new SdrfParserService(sdrfFile);
 * parser.setOnSucceeded(e -> {
 *     SdrfData data = parser.getValue();
 *     // Use extracted metadata
 * });
 * parser.start();
 * </pre>
 */
public class SdrfParserService extends Service<SdrfParserService.SdrfData> {

    private static final Logger logger = LoggerFactory.getLogger(SdrfParserService.class);

    // Column name patterns
    private static final Pattern CHARACTERISTIC_PATTERN = Pattern.compile(
            "characteristics\\s*\\[\\s*([^\\]]+)\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "comment\\s*\\[\\s*([^\\]]+)\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONTOLOGY_PATTERN = Pattern.compile(
            "(.+?)\\s*\\(\\s*([A-Za-z]+):([^)]+)\\s*\\)");
    private static final Pattern NT_PATTERN = Pattern.compile(
            "NT=([^;]+)(?:;|$)");
    private static final Pattern AC_PATTERN = Pattern.compile(
            "AC=([^;]+)(?:;|$)");

    private final File sdrfFile;

    public SdrfParserService(File sdrfFile) {
        this.sdrfFile = sdrfFile;
    }

    @Override
    protected Task<SdrfData> createTask() {
        return new SdrfParseTask(sdrfFile);
    }

    /**
     * Parsed SDRF data
     */
    public record SdrfData(
            List<SdrfRow> rows,
            Set<CvParam> organisms,
            Set<CvParam> tissues,
            Set<CvParam> cellTypes,
            Set<CvParam> diseases,
            Set<CvParam> instruments,
            Set<CvParam> modifications,
            Set<CvParam> labels,
            List<String> sampleNames,
            List<String> sourceNames,
            Map<String, Set<String>> rawValues  // Column name -> unique values
    ) {
        public boolean isEmpty() {
            return rows == null || rows.isEmpty();
        }

        public int getSampleCount() {
            return rows != null ? rows.size() : 0;
        }

        public boolean hasOrganism() {
            return organisms != null && !organisms.isEmpty();
        }

        public boolean hasTissue() {
            return tissues != null && !tissues.isEmpty();
        }

        public boolean hasInstrument() {
            return instruments != null && !instruments.isEmpty();
        }
    }

    /**
     * Single SDRF row with extracted metadata
     */
    public record SdrfRow(
            String sourceName,
            String sampleName,
            Map<String, String> characteristics,
            Map<String, String> comments,
            Map<String, CvParam> parsedTerms
    ) {}

    /**
     * Parse task implementation
     */
    private static class SdrfParseTask extends Task<SdrfData> {

        private final File file;

        public SdrfParseTask(File file) {
            this.file = file;
        }

        @Override
        protected SdrfData call() throws Exception {
            logger.info("Parsing SDRF file: {}", file.getName());
            updateMessage("Reading SDRF file...");

            if (!file.exists()) {
                throw new FileNotFoundException("SDRF file not found: " + file.getAbsolutePath());
            }

            List<String[]> rawRows = new ArrayList<>();
            String[] headers = null;

            // Read file
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\t", -1);

                    if (firstLine) {
                        headers = parts;
                        firstLine = false;
                    } else {
                        rawRows.add(parts);
                    }
                }
            }

            if (headers == null || rawRows.isEmpty()) {
                logger.warn("SDRF file is empty or invalid");
                return new SdrfData(
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyMap()
                );
            }

            logger.info("Found {} rows with {} columns", rawRows.size(), headers.length);
            updateMessage("Processing " + rawRows.size() + " samples...");

            // Index columns
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].toLowerCase().trim(), i);
            }

            // Parse rows
            List<SdrfRow> rows = new ArrayList<>();
            Set<CvParam> organisms = new LinkedHashSet<>();
            Set<CvParam> tissues = new LinkedHashSet<>();
            Set<CvParam> cellTypes = new LinkedHashSet<>();
            Set<CvParam> diseases = new LinkedHashSet<>();
            Set<CvParam> instruments = new LinkedHashSet<>();
            Set<CvParam> modifications = new LinkedHashSet<>();
            Set<CvParam> labels = new LinkedHashSet<>();
            List<String> sampleNames = new ArrayList<>();
            List<String> sourceNames = new ArrayList<>();
            Map<String, Set<String>> rawValues = new HashMap<>();

            int rowNum = 0;
            for (String[] parts : rawRows) {
                rowNum++;
                updateProgress(rowNum, rawRows.size());

                Map<String, String> characteristics = new HashMap<>();
                Map<String, String> comments = new HashMap<>();
                Map<String, CvParam> parsedTerms = new HashMap<>();

                String sourceName = "";
                String sampleName = "";

                for (int i = 0; i < headers.length && i < parts.length; i++) {
                    String header = headers[i].trim();
                    String value = parts[i].trim();
                    String headerLower = header.toLowerCase();

                    if (value.isEmpty()) continue;

                    // Track raw values
                    rawValues.computeIfAbsent(header, k -> new LinkedHashSet<>()).add(value);

                    // Source name
                    if (headerLower.equals("source name")) {
                        sourceName = value;
                        sourceNames.add(value);
                        continue;
                    }

                    // Sample name
                    if (headerLower.contains("sample name")) {
                        sampleName = value;
                        sampleNames.add(value);
                        continue;
                    }

                    // Parse characteristics
                    Matcher charMatcher = CHARACTERISTIC_PATTERN.matcher(header);
                    if (charMatcher.find()) {
                        String charType = charMatcher.group(1).trim().toLowerCase();
                        characteristics.put(charType, value);

                        CvParam parsed = parseOntologyValue(value);
                        if (parsed != null) {
                            parsedTerms.put(charType, parsed);

                            // Add to appropriate collection
                            switch (charType) {
                                case "organism" -> organisms.add(parsed);
                                case "organism part", "tissue" -> tissues.add(parsed);
                                case "cell type", "cell line" -> cellTypes.add(parsed);
                                case "disease" -> diseases.add(parsed);
                            }
                        }
                        continue;
                    }

                    // Parse comments
                    Matcher commentMatcher = COMMENT_PATTERN.matcher(header);
                    if (commentMatcher.find()) {
                        String commentType = commentMatcher.group(1).trim().toLowerCase();
                        comments.put(commentType, value);

                        CvParam parsed = parseOntologyValue(value);
                        if (parsed != null) {
                            parsedTerms.put(commentType, parsed);

                            // Add to appropriate collection
                            switch (commentType) {
                                case "instrument" -> instruments.add(parsed);
                                case "modification parameters", "modification" -> modifications.add(parsed);
                                case "label" -> labels.add(parsed);
                            }
                        }
                        continue;
                    }
                }

                rows.add(new SdrfRow(sourceName, sampleName, characteristics, comments, parsedTerms));
            }

            logger.info("Parsed SDRF: {} organisms, {} tissues, {} instruments",
                    organisms.size(), tissues.size(), instruments.size());

            return new SdrfData(
                    rows,
                    organisms,
                    tissues,
                    cellTypes,
                    diseases,
                    instruments,
                    modifications,
                    labels,
                    sampleNames,
                    sourceNames,
                    rawValues
            );
        }

        /**
         * Parse ontology-annotated value like "Homo sapiens (NCBITaxon:9606)"
         * or MAGE-TAB format like "NT=Homo sapiens;AC=NCBITaxon:9606"
         */
        private CvParam parseOntologyValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            // Try standard format: "term name (ONTOLOGY:accession)"
            Matcher matcher = ONTOLOGY_PATTERN.matcher(value);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                String ontology = matcher.group(2).trim();
                String accession = matcher.group(3).trim();

                String cvLabel = mapOntologyName(ontology);
                String fullAccession = ontology + ":" + accession;

                return new CvParam(cvLabel, fullAccession, name, null);
            }

            // Try MAGE-TAB format: "NT=name;AC=accession"
            Matcher ntMatcher = NT_PATTERN.matcher(value);
            Matcher acMatcher = AC_PATTERN.matcher(value);

            if (ntMatcher.find() && acMatcher.find()) {
                String name = ntMatcher.group(1).trim();
                String accession = acMatcher.group(1).trim();

                String cvLabel = "UNKNOWN";
                if (accession.contains(":")) {
                    cvLabel = mapOntologyName(accession.substring(0, accession.indexOf(":")));
                }

                return new CvParam(cvLabel, accession, name, null);
            }

            // Just a plain value without ontology annotation
            return new CvParam("UNKNOWN", "", value.trim(), null);
        }

        /**
         * Map ontology prefix to CV label
         */
        private String mapOntologyName(String ontology) {
            return switch (ontology.toUpperCase()) {
                case "NCBITAXON" -> "NEWT";
                case "BTO" -> "BTO";
                case "CL" -> "CL";
                case "DOID" -> "DOID";
                case "MS" -> "MS";
                case "MOD", "UNIMOD" -> "MOD";
                case "EFO" -> "EFO";
                case "PRIDE" -> "PRIDE";
                default -> ontology.toUpperCase();
            };
        }
    }

    /**
     * Static method to check if a file looks like an SDRF
     */
    public static boolean isSdrfFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        if (name.contains("sdrf") && name.endsWith(".tsv")) {
            return true;
        }

        // Check first line for SDRF-like headers
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            if (firstLine != null) {
                String lower = firstLine.toLowerCase();
                return lower.contains("source name") ||
                       lower.contains("characteristics[") ||
                       lower.contains("comment[");
            }
        } catch (IOException e) {
            logger.debug("Could not check file for SDRF format: {}", file.getName());
        }

        return false;
    }

    /**
     * Synchronous parse method for simple use cases
     */
    public static SdrfData parse(File file) throws Exception {
        SdrfParseTask task = new SdrfParseTask(file);
        return task.call();
    }
}
