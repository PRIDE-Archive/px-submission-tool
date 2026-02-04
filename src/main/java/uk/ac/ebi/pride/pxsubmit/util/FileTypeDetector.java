package uk.ac.ebi.pride.pxsubmit.util;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Enhanced file type detection utility.
 * Detects file types based on extensions, content, and tool-specific patterns.
 *
 * File Categories (per PRIDE Guidelines):
 * - RAW: Instrument raw data (.raw, .wiff, .d, .baf, .tdf, .mzML, .mzXML)
 * - RESULT (STANDARD): mzIdentML, mzTab
 * - SEARCH (ANALYSIS): Tool outputs (MaxQuant, DIA-NN, FragPipe, etc.)
 * - PEAK: Peak lists (.mgf, .dta, .pkl)
 * - EXPERIMENTAL_DESIGN (SDRF): Sample metadata
 * - OTHER: FASTA, spectral libraries, supplementary files
 */
public class FileTypeDetector {

    private static final Logger logger = LoggerFactory.getLogger(FileTypeDetector.class);

    // ==================== Extension Mappings ====================

    /** RAW file extensions (instrument data) */
    private static final Set<String> RAW_EXTENSIONS = Set.of(
            // Thermo & Waters
            "raw",
            // SCIEX
            "wiff", "wiff2", "wiff.scan",
            // Bruker & Agilent
            "d", "baf", "tdf", "tdf_bin",
            // mzML/mzXML (can be raw or processed)
            "mzml", "mzxml",
            // Others
            "yep", "fid", "lcd"
    );

    /** Result/Standard file extensions */
    private static final Set<String> RESULT_EXTENSIONS = Set.of(
            "mzid", "mzidentml",
            "mztab"
    );

    /** Peak list file extensions */
    private static final Set<String> PEAK_EXTENSIONS = Set.of(
            "mgf",
            "dta",
            "pkl", "pklbin",
            "ms2",
            "apl"
    );

    /** FASTA/database file extensions */
    private static final Set<String> FASTA_EXTENSIONS = Set.of(
            "fasta", "fa", "faa", "fas",
            "peff"
    );

    /** Spectral library file extensions */
    private static final Set<String> LIBRARY_EXTENSIONS = Set.of(
            "blib", "sptxt", "msp", "splib", "nist",
            "dlib", "elib", "speclib"
    );

    /** SDRF/experimental design file patterns */
    private static final Set<String> SDRF_PATTERNS = Set.of(
            "sdrf", "experimental_design", "experiment_design"
    );

    /** Search engine output file extensions (tool-specific) */
    private static final Set<String> SEARCH_EXTENSIONS = Set.of(
            // Generic analysis outputs
            "txt", "tsv", "csv",
            "parquet",
            // Mascot
            "dat",
            // Proteome Discoverer
            "pdresult", "msf",
            // PEAKS
            "pepxml", "pep.xml",
            // Scaffold
            "sf3",
            // X!Tandem
            "t.xml", "tandem"
    );

    // ==================== Tool Detection ====================

    /**
     * Detected analysis tool with expected files
     */
    public enum AnalysisTool {
        MAXQUANT("MaxQuant", List.of("evidence.txt", "peptides.txt", "proteinGroups.txt", "msms.txt"),
                List.of("mqpar.xml", "summary.txt", "allPeptides.txt", "modificationSpecificPeptides.txt")),

        DIANN("DIA-NN", List.of("report.tsv"),
                List.of("report.parquet", "report.pr_matrix.tsv", "report.pg_matrix.tsv", "report.stats.tsv")),

        FRAGPIPE("FragPipe", List.of("psm.tsv", "protein.tsv"),
                List.of("peptide.tsv", "ion.tsv", "combined_protein.tsv", "fragpipe.workflow", "fragpipe-files.fp-manifest")),

        SPECTRONAUT("Spectronaut", List.of(),
                List.of("Report.tsv", "Report.xls")),

        MASCOT("Mascot", List.of("*.dat"),
                List.of()),

        PROTEOME_DISCOVERER("Proteome Discoverer", List.of("*.pdresult"),
                List.of("*.msf")),

        SKYLINE("Skyline", List.of("*.sky"),
                List.of("*.sky.zip", "*.skyd", "*.blib")),

        OPENMS("OpenMS", List.of(),
                List.of("*.consensusXML", "*.idXML", "*.featureXML")),

        UNKNOWN("Unknown", List.of(), List.of());

        private final String displayName;
        private final List<String> requiredPatterns;
        private final List<String> optionalPatterns;

        AnalysisTool(String displayName, List<String> requiredPatterns, List<String> optionalPatterns) {
            this.displayName = displayName;
            this.requiredPatterns = requiredPatterns;
            this.optionalPatterns = optionalPatterns;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getRequiredPatterns() {
            return requiredPatterns;
        }

        public List<String> getOptionalPatterns() {
            return optionalPatterns;
        }
    }

    /**
     * Result of tool detection
     */
    public record ToolDetectionResult(
            AnalysisTool tool,
            List<String> foundRequiredFiles,
            List<String> missingRequiredFiles,
            List<String> foundOptionalFiles,
            double confidence
    ) {
        public boolean isConfident() {
            return confidence >= 0.7;
        }
    }

    // ==================== Detection Methods ====================

    /**
     * Detect file type from a file
     */
    public static ProjectFileType detectFileType(File file) {
        if (file == null || !file.exists()) {
            return ProjectFileType.OTHER;
        }

        String fileName = file.getName().toLowerCase();
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();

        // Check for SDRF patterns first (before generic .tsv)
        if (isSdrfFile(fileName)) {
            return ProjectFileType.EXPERIMENTAL_DESIGN;
        }

        // Check for FASTA files
        if (FASTA_EXTENSIONS.contains(extension)) {
            return ProjectFileType.OTHER; // FASTA goes to OTHER category
        }

        // Check for spectral library files
        if (LIBRARY_EXTENSIONS.contains(extension)) {
            return ProjectFileType.OTHER; // Libraries go to OTHER
        }

        // Check for result/standard files (mzIdentML, mzTab)
        if (RESULT_EXTENSIONS.contains(extension)) {
            return ProjectFileType.RESULT;
        }

        // Check for peak list files
        if (PEAK_EXTENSIONS.contains(extension)) {
            return ProjectFileType.PEAK;
        }

        // Check for RAW files
        if (RAW_EXTENSIONS.contains(extension)) {
            // mzML can be raw or processed - check content if needed
            if ("mzml".equals(extension) || "mzxml".equals(extension)) {
                return ProjectFileType.RAW; // Default to RAW for mzML
            }
            return ProjectFileType.RAW;
        }

        // Try to detect using MassSpecFileFormat (only for files with extensions)
        if (!extension.isEmpty()) {
            try {
                MassSpecFileFormat format = MassSpecFileFormat.checkFormat(file);
                if (format != null) {
                    return switch (format) {
                        case PRIDE, MZIDENTML, MZTAB -> ProjectFileType.RESULT;
                        case MZML, INDEXED_MZML -> ProjectFileType.RAW;
                        default -> ProjectFileType.OTHER;
                    };
                }
            } catch (Exception e) {
                // Catch any exception (IOException, FileNotFoundException, IllegalArgumentException, etc.)
                logger.debug("Could not check file format for {}: {}", fileName, e.getMessage());
            }
        }

        // Check for search/analysis files by extension
        if (SEARCH_EXTENSIONS.contains(extension)) {
            // Could be analysis output - need context to determine
            return ProjectFileType.SEARCH;
        }

        // Default to OTHER
        return ProjectFileType.OTHER;
    }

    /**
     * Detect file type from a DataFile
     */
    public static ProjectFileType detectFileType(DataFile dataFile) {
        if (dataFile == null) {
            return ProjectFileType.OTHER;
        }
        return detectFileType(dataFile.getFile());
    }

    /**
     * Check if file is an SDRF/experimental design file
     */
    public static boolean isSdrfFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        for (String pattern : SDRF_PATTERNS) {
            if (lowerName.contains(pattern)) {
                return true;
            }
        }
        // Check for specific SDRF file format: sdrf*.tsv or *.sdrf.tsv
        return lowerName.startsWith("sdrf") && lowerName.endsWith(".tsv") ||
               lowerName.endsWith(".sdrf.tsv");
    }

    /**
     * Check if file is a FASTA database file
     */
    public static boolean isFastaFile(File file) {
        if (file == null) return false;
        String ext = FilenameUtils.getExtension(file.getName()).toLowerCase();
        return FASTA_EXTENSIONS.contains(ext);
    }

    /**
     * Check if file is a spectral library
     */
    public static boolean isSpectralLibrary(File file) {
        if (file == null) return false;
        String ext = FilenameUtils.getExtension(file.getName()).toLowerCase();
        return LIBRARY_EXTENSIONS.contains(ext);
    }

    /**
     * Detect analysis tool from a collection of files
     */
    public static ToolDetectionResult detectTool(Collection<DataFile> files) {
        if (files == null || files.isEmpty()) {
            return new ToolDetectionResult(AnalysisTool.UNKNOWN, List.of(), List.of(), List.of(), 0.0);
        }

        Set<String> fileNames = new HashSet<>();
        for (DataFile df : files) {
            if (df.getFile() != null) {
                fileNames.add(df.getFile().getName().toLowerCase());
            }
        }

        // Try each tool in order of specificity
        ToolDetectionResult best = null;
        double bestConfidence = 0;

        for (AnalysisTool tool : AnalysisTool.values()) {
            if (tool == AnalysisTool.UNKNOWN) continue;

            ToolDetectionResult result = checkTool(tool, fileNames);
            if (result.confidence() > bestConfidence) {
                bestConfidence = result.confidence();
                best = result;
            }
        }

        return best != null ? best : new ToolDetectionResult(AnalysisTool.UNKNOWN, List.of(), List.of(), List.of(), 0.0);
    }

    private static ToolDetectionResult checkTool(AnalysisTool tool, Set<String> fileNames) {
        List<String> foundRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        List<String> foundOptional = new ArrayList<>();

        // Check required files
        for (String pattern : tool.getRequiredPatterns()) {
            if (matchesPattern(pattern, fileNames)) {
                foundRequired.add(pattern);
            } else {
                missingRequired.add(pattern);
            }
        }

        // Check optional files
        for (String pattern : tool.getOptionalPatterns()) {
            if (matchesPattern(pattern, fileNames)) {
                foundOptional.add(pattern);
            }
        }

        // Calculate confidence
        double confidence = 0.0;
        int totalRequired = tool.getRequiredPatterns().size();
        int totalOptional = tool.getOptionalPatterns().size();

        if (totalRequired > 0) {
            confidence = (double) foundRequired.size() / totalRequired;
            // Boost for optional files
            if (totalOptional > 0 && !foundOptional.isEmpty()) {
                confidence += 0.2 * ((double) foundOptional.size() / totalOptional);
            }
        } else if (!foundOptional.isEmpty()) {
            // No required files, use optional
            confidence = 0.5 * ((double) foundOptional.size() / totalOptional);
        }

        return new ToolDetectionResult(tool, foundRequired, missingRequired, foundOptional, Math.min(1.0, confidence));
    }

    private static boolean matchesPattern(String pattern, Set<String> fileNames) {
        String lowerPattern = pattern.toLowerCase();

        // Handle wildcard patterns
        if (lowerPattern.startsWith("*.")) {
            String ext = lowerPattern.substring(2);
            for (String fileName : fileNames) {
                if (fileName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }

        // Exact match
        return fileNames.contains(lowerPattern);
    }

    // ==================== Utility Methods ====================

    /**
     * Get display name for a file type (following PRIDE Guidelines terminology)
     */
    public static String getDisplayName(ProjectFileType type) {
        return switch (type) {
            case RAW -> "RAW Files";
            case RESULT -> "STANDARD File Formats";  // mzIdentML, mzTab
            case SEARCH -> "ANALYSIS Files";         // Tool outputs (MaxQuant, DIA-NN, etc.)
            case PEAK -> "Peak Lists";
            case EXPERIMENTAL_DESIGN -> "Metadata (SDRF)";
            case MS_IMAGE_DATA -> "MS Imaging Data";
            case GEL -> "Gel Images";
            case OTHER -> "Other Files";
            default -> type.name();
        };
    }

    /**
     * Get color for a file type (for UI badges)
     */
    public static String getColor(ProjectFileType type) {
        return switch (type) {
            case RAW -> "#0066cc";              // Blue
            case RESULT -> "#6f42c1";           // Purple
            case SEARCH -> "#28a745";           // Green
            case PEAK -> "#fd7e14";             // Orange
            case EXPERIMENTAL_DESIGN -> "#17a2b8"; // Cyan
            case MS_IMAGE_DATA -> "#e83e8c";    // Pink
            case GEL -> "#6c757d";              // Gray
            case OTHER -> "#6c757d";            // Gray
            default -> "#6c757d";
        };
    }

    /**
     * Check if a file type is mandatory for submission
     */
    public static boolean isMandatory(ProjectFileType type) {
        return type == ProjectFileType.RAW;
    }

    /**
     * Check if a file type is recommended for complete submission
     */
    public static boolean isRecommended(ProjectFileType type) {
        return type == ProjectFileType.RESULT ||
               type == ProjectFileType.SEARCH ||
               type == ProjectFileType.EXPERIMENTAL_DESIGN;
    }

    /**
     * Get all extensions for a file type
     */
    public static Set<String> getExtensions(ProjectFileType type) {
        return switch (type) {
            case RAW -> RAW_EXTENSIONS;
            case RESULT -> RESULT_EXTENSIONS;
            case SEARCH -> SEARCH_EXTENSIONS;
            case PEAK -> PEAK_EXTENSIONS;
            case EXPERIMENTAL_DESIGN -> Set.of("tsv", "txt");
            default -> Set.of();
        };
    }
}
